package com.dongah.fastcharger.smartro;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.DateTimeConvert;
import com.dongah.fastcharger.basefunction.GlobalVariables;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import service.vcat.smartro.com.vcat.SmartroVCatCallback;
import service.vcat.smartro.com.vcat.SmartroVCatInterface;

/**
 * V-CAT AIDL 바인딩 및 결제/취소 싱글톤
 * Fragment에 종속되지 않고 어디서든 V-CAT 결제/취소를 수행할 수 있도록
 * Application Context 기반으로 동작
 *
 * 사용처
 * NonMemberPhoneAuthHandler  : p2.resp error_type=3 → 미처리 부분취소
 * PartCancelResultHandler    : q2.resp error_type=3 → 미처리 부분취소
 * Fragment (ServiceProcessingFragment 대체 가능)
 *
 * 생명 주기
 * MainActivity.onCreate()  → VCatPaymentManager.getInstance().bind(context)
 * MainActivity.onDestroy() → VCatPaymentManager.getInstance().unbind()
 *
 * ── 사용 예시 ───────────────────────────────────────────
 *   VCatPaymentManager.getInstance()
 *       .requestPartCancel(chargingCurrentData, cfg, connectorId, callback);
 */
public class VCatPaymentManager {
    private static final Logger logger = LoggerFactory.getLogger(VCatPaymentManager.class);

    private static final String VCAT_PACKAGE = "service.vcat.smartro.com.vcat";
    private static final String VCAT_ACTION  = "smartro.vcat.action";

    // 싱글톤
    private static volatile VCatPaymentManager instance;

    public static VCatPaymentManager getInstance() {
        if (instance == null) {
            synchronized (VCatPaymentManager.class) {
                if (instance == null) instance = new VCatPaymentManager();
            }
        }
        return instance;
    }

    private VCatPaymentManager() {}

    private volatile SmartroVCatInterface vcatInterface = null;
    private boolean isBound = false;
    private Context appContext = null;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final int REQ_VCAT_AWAKE = 200;
    private AppCompatActivity mActivity = null;
    private boolean mSetupDone = false;
    private String mDeviceSerial = ""; // 매핑할 SMT-B420 시리얼 번호

    public void setDeviceSerial(String serial) {
        this.mDeviceSerial = serial != null ? serial.trim() : "";
        logger.info("VCatPaymentManager: 장치 시리얼 설정 → {}", mDeviceSerial);
    }

    // 결제 결과 Callback interface
    public interface PaymentCallback {
        /** 승인 성공 (service-result=0000, response-code=00) */
        void onSuccess(JSONObject result);
        /** VAN 거절 또는 V-CAT 오류 */
        void onFailure(String errorMessage);
    }

    // ── 바인딩 관리 ───────────────────────────────────────
    /**
     *  V-CAT 서비스 바인딩
     *  MainActivity.onCreate() 에서 호출
     *
     *  AppCompatActivity 를 받아 onActivityResult 위임을 처리합니다.
     *  Context 만 넘기는 기존 호출부가 있다면 아래 bind(Context) 오버로드를 사용하세요.
     */
    public void bind(AppCompatActivity activity) {
        mActivity = activity;
        appContext = activity.getApplicationContext();
        if (isBound) return;
        awakeCat(activity); // startActivityForResult 방식 (결과 → onVCatAwakeResult)
    }

    /**
     * V-CAT 서비스 언바인딩
     * MainActivity.onDestroy() 에서 호출
     */
    public void bind(Context context) {
        appContext = context.getApplicationContext();
        if (isBound) return;
        awakeCat(null);
    }

    public void onVCatAwakeResult() {
        logger.info("VCatPaymentManager: V-CAT 강제 실행 완료 → bindService 진행");
        doBindService();
    }

    public int getAwakeRequestCode() { return REQ_VCAT_AWAKE; }

    public void unbind() {
        if (isBound && appContext != null) {
            try {
                appContext.unbindService(serviceConnection);
            } catch (Exception e) {
                logger.error("VCatPaymentManager: unbindService 오류: {}", e.getMessage());
            } finally {
                vcatInterface = null;
                isBound       = false;
                mSetupDone    = false;
                mActivity     = null;
            }
        }
    }

    public boolean isConnected() {
        return isBound && vcatInterface != null;
    }

    public boolean isSetupDone() { return mSetupDone; }

    public SmartroVCatInterface getVcatInterface() {
        return vcatInterface;
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            vcatInterface = SmartroVCatInterface.Stub.asInterface(service);
            isBound = true;
            logger.info("VCatPaymentManager: V-CAT 연결 성공 → SMT-B420 초기 설정 시작");
            mainHandler.post(() -> setupStep3_scanAndSetDevice());
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            vcatInterface = null;
            isBound = false;
            mSetupDone = false;
            logger.warn("VCatPaymentManager: V-CAT 연결 끊김");
        }
    };

    private void awakeCat(AppCompatActivity activity) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("smartroapp://vcatscheme?manage=awake"));
            if (activity != null) {
                // onActivityResult 위임 방식 (MainActivity 에서 onVCatAwakeResult 호출 필요)
                intent.setFlags(0);
                activity.startActivityForResult(intent, REQ_VCAT_AWAKE);
            } else {
                // Context 만 있는 경우: 강제 실행 후 바로 bindService
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (appContext != null) appContext.startActivity(intent);
                doBindService();
            }
        } catch (Exception e) {
            logger.warn("awakeCat skipped: {} → 바로 bindService 진행", e.getMessage());
            doBindService();
        }
    }

    private void doBindService() {
        if (appContext == null) return;
        Intent intent = new Intent(VCAT_ACTION);
        intent.setPackage(VCAT_PACKAGE);
        intent.putExtra("packageName", appContext.getPackageName());
        boolean ok = appContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        if (!ok) logger.error("VCatPaymentManager: bindService 실패");
        else      logger.info("VCatPaymentManager: bindService 요청");
    }

    // ─────────────────────────────────────────────────────
    // SMT-B420 초기 설정 시퀀스 (onServiceConnected 이후 자동 실행)
    // ─────────────────────────────────────────────────────
    /**
     * [3/6] 블루투스 장치 스캔 → 시리얼 번호로 매핑 → 장치 설정
     *
     * indicate 로 현재 연결 가능한 BT 드라이버 목록을 가져온 뒤,
     * mDeviceSerial 을 포함하는 항목을 찾아 device-comm 에 등록합니다.
     *
     * 시리얼이 비어 있으면 스캔 없이 목록 첫 번째 항목(auto-detection 제외)을 사용합니다.
     */
    private void setupStep3_scanAndSetDevice() {
        logger.info("[3/6] BT 장치 스캔 시작 (매핑 시리얼={})", mDeviceSerial);
        if (vcatInterface == null) {
            logger.error("[3/6] vcatInterface null");
            return;
        }
        try {
            // indicate 로 블루투스 드라이버 목록 요청
            JSONObject req = new JSONObject();
            req.put("service",   "indicate");
            req.put("available", "bt");

            vcatInterface.executeService(req.toString(), new SmartroVCatCallback.Stub() {
                @Override
                public void onServiceEvent(String json) {}

                @Override
                public void onServiceResult(String json) {
                    mainHandler.post(() -> {
                        try {
                            JSONObject result = new JSONObject(json);
                            String code = result.optString("service-result", "");
                            if (!"0000".equals(code)) {
                                logger.error("[3/6] BT 스캔 실패 [{}]: {}",
                                        code, result.optString("service-description"));
                                // 스캔 실패 시 장치 설정 건너뛰고 키 교환 진행
                                setupStep4_exchangeKey();
                                return;
                            }

                            // 응답에서 BT 드라이버 목록 추출 ("bt" 키)
                            JSONArray btList = result.optJSONArray("bt");
                            if (btList == null || btList.length() == 0) {
                                logger.warn("[3/6] 주변에 BT 장치가 없습니다.");
                                setupStep4_exchangeKey();
                                return;
                            }

                            logger.info("[3/6] BT 장치 목록: {}", btList);

                            // 시리얼 번호가 포함된 항목 탐색
                            String matched = null;
                            for (int i = 0; i < btList.length(); i++) {
                                String entry = btList.getString(i);
                                if (entry.equalsIgnoreCase("auto-detection")) continue;

                                // 시리얼 미설정 → 첫 번째 항목 사용
                                if (mDeviceSerial.isEmpty()) {
                                    matched = entry;
                                    logger.info("[3/6] 시리얼 미설정 → 첫 번째 항목 사용: {}", matched);
                                    break;
                                }

                                // 시리얼 번호를 드라이버 식별값에서 검색 (대소문자 무시)
                                if (entry.toLowerCase().contains(mDeviceSerial.toLowerCase())) {
                                    matched = entry;
                                    logger.info("[3/6] 시리얼 매핑 성공: {} → {}", mDeviceSerial, matched);
                                    break;
                                }
                            }

                            if (matched == null) {
                                logger.error("[3/6] 시리얼 [{}] 에 해당하는 BT 장치를 찾지 못했습니다." +
                                        " 목록: {}", mDeviceSerial, btList);
                                // 찾지 못해도 키 교환은 진행
                                setupStep4_exchangeKey();
                                return;
                            }

                            // 찾은 드라이버 식별값으로 장치 설정
                            setupStep3b_setDevice(matched);

                        } catch (Exception e) {
                            logger.error("[3/6] BT 스캔 결과 파싱 오류: {}", e.getMessage());
                            setupStep4_exchangeKey();
                        }
                    });
                }
            });
        } catch (Exception e) {
            logger.error("[3/6] BT 스캔 요청 오류: {}", e.getMessage());
            setupStep4_exchangeKey();
        }
    }
    /** [3b/6] 매핑된 드라이버 식별값으로 setting 요청 */
    private void setupStep3b_setDevice(String driverKey) {
        logger.info("[3b/6] 장치 설정 → driver-key: {}", driverKey);
        try {
            JSONObject req = new JSONObject();
            req.put("service", "setting");
            req.put("device",  "dongle");
            req.put("device-comm", new JSONArray().put("bt").put(driverKey));

            executeSetup(req.toString(), "[3b/6] 장치 설정", result -> {
                logger.info("[3b/6] 장치 설정 완료 (driver={})", driverKey);
                setupStep4_exchangeKey();
            });
        } catch (Exception e) {
            logger.error("[3b/6] 장치 설정 JSON 오류: {}", e.getMessage());
            setupStep4_exchangeKey();
        }
    }

    /** [4/6] 키 교환 (신규 장비 필수) */
    private void setupStep4_exchangeKey() {
        logger.info("[4/6] 키 교환 시작");
        try {
            ChargerConfiguration cfg = ((MainActivity) MainActivity.mContext).getChargerConfiguration();
            JSONObject req = new JSONObject();
            req.put("service",       "function");
            req.put("cat-id",        cfg.getMID());
            req.put("business-no",   cfg.getBizNo());
            req.put("device-manage", "exchange-key");
            executeSetup(req.toString(), "[4/6] 키 교환", result -> {
                logger.info("[4/6] 키 교환 완료");
                setupStep5_checkIntegrity();
            });
        } catch (Exception e) {
            logger.error("[4/6] 키 교환 JSON 오류: {}", e.getMessage());
        }
    }

    /** [5/6] 무결성 점검 */
    private void setupStep5_checkIntegrity() {
        logger.info("[5/6] 무결성 점검 시작");
        try {
            JSONObject req = new JSONObject();
            req.put("service",       "function");
            req.put("device-manage", "check-integrity");
            executeSetup(req.toString(), "[5/6] 무결성 점검", result -> {
                logger.info("[5/6] 무결성 점검 완료");
                setupStep6_getDeviceInfo();
            });
        } catch (Exception e) {
            logger.error("[5/6] 무결성 점검 JSON 오류: {}", e.getMessage());
        }
    }

    /** [6/6] 장치 정보 확인 */
    private void setupStep6_getDeviceInfo() {
        logger.info("[6/6] 장치 정보 확인");
        try {
            JSONObject req = new JSONObject();
            req.put("service",       "function");
            req.put("device-manage", "get-info");
            executeSetup(req.toString(), "[6/6] 장치 정보 확인", result -> {
                String name   = result.optString("device-name",   "N/A");
                String serial = result.optString("device-serial",  "N/A");
                logger.info("SMT-B420 초기 설정 완료! 장치명={}, 시리얼={}", name, serial);
                mSetupDone = true;
            });
        } catch (Exception e) {
            logger.error("[6/6] 장치 정보 확인 JSON 오류: {}", e.getMessage());
        }
    }

    /** 초기 설정 전용 executeService 헬퍼 (실패 시 로그만 남기고 다음 단계 진행) */
    private void executeSetup(String requestJson, String label,
                              SetupSuccessCallback onSuccess) {
        if (vcatInterface == null) {
            logger.error("executeSetup [{}]: vcatInterface null", label);
            return;
        }
        try {
            vcatInterface.executeService(requestJson, new SmartroVCatCallback.Stub() {
                @Override
                public void onServiceEvent(String json) {}

                @Override
                public void onServiceResult(String json) {
                    mainHandler.post(() -> {
                        try {
                            JSONObject result = new JSONObject(json);
                            String code = result.optString("service-result", "");
                            if ("0000".equals(code)) {
                                onSuccess.run(result);
                            } else {
                                String desc = result.optString("service-description", "알 수 없는 오류");
                                logger.error(" {} 실패 [{}]: {}", label, code, desc);
                                // 설정 실패해도 다음 단계 계속 시도
                                if (label.contains("[4/6]")) setupStep5_checkIntegrity();
                                else if (label.contains("[5/6]")) setupStep6_getDeviceInfo();
                                else if (label.contains("[6/6]")) mSetupDone = true;
                            }
                        } catch (Exception e) {
                            logger.error("executeSetup [{}] 결과 파싱 오류: {}", label, e.getMessage());
                        }
                    });
                }
            });
        } catch (Exception e) {
            logger.error("executeSetup [{}] 호출 예외: {}", label, e.getMessage());
        }
    }

    @FunctionalInterface
    private interface SetupSuccessCallback {
        void run(JSONObject result) throws Exception;
    }


    // ─────────────────────────────────────────────────────
    // 미처리 부분취소 처리 (p2.resp / q2.resp error_type=3)
    // ─────────────────────────────────────────────────────

    /**
     * 미처리 부분취소 V-CAT 요청
     *
     *  호출 조건
     *     - p2.resp (NonMemberPhoneAuthHandler) error_type=3
     *     - q2.resp (PartCancelResultHandler)   error_type=3
     * @param d             ChargingCurrentData (usePoint, limitApplyTid, approvalDate 등 세팅된 상태)
     * @param cfg           ChargerConfiguration
     * @param connectorId   OCPP connector ID (q2.req 재전송에 사용)
     * @param sendQ2        true  → V-CAT 완료 후 q2.req(PartCancelResult) 자동 전송
     *                      false → q2.req 전송 없이 callback 만 호출 (p2 경로에서 홈 이동 시)
     * @param callback      결과 콜백 (메인 스레드에서 호출)
     */
    public void requestPendingPartCancel(ChargingCurrentData d,
                                         ChargerConfiguration cfg,
                                         int connectorId,
                                         boolean sendQ2,
                                         PaymentCallback callback) {
        if (!isConnected()) {
            logger.error("requestPendingPartCancel: V-CAT 미연결");
            mainHandler.post(() -> callback.onFailure("V-CAT 서비스에 연결되지 않았습니다."));
            return;
        }

        try {
            int cancelAmount = d.getUsePoint();                 // CSMS 가 전달한 취소 금액
            int surtax       = (cancelAmount * 10) / 110;
            // 원승인번호: limitApplyTid 우선, 없으면 approvalNo
            String approvalNo = (d.getLimitApplyTid() != null && !d.getLimitApplyTid().isEmpty())
                    ? d.getLimitApplyTid() : d.getApprovalNo();
            // 원승인일자: limitApplyDt 에서 추출 (YYYY-MM-DDTHH:mm:ssZ → YYmmdd)
            DateTimeConvert dtc     = new DateTimeConvert();
            String approvalDate     = extractYYmmdd(dtc, d.getLimitApplyDt(), d.getApprovalDate());

            JSONObject req = new JSONObject();
            req.put("service",      "payment");
            req.put("type",         "credit");
            req.put("deal",         "partial-cancel");
            req.put("cat-id",       cfg.getMID());
            req.put("business-no",  cfg.getBizNo());
            req.put("total-amount", String.valueOf(cancelAmount));
            req.put("surtax",       String.valueOf(surtax));
            req.put("tip",          "0");
            req.put("approval-no",  approvalNo);
            req.put("approval-date",approvalDate);
            req.put("pg-tran-seq",  d.getLimitApplySeq());
            req.put("member-type",  "pg");
            logger.info("미처리 부분취소 V-CAT 요청: cancel={}, approval_no={}, sendQ2={}",
                    cancelAmount, approvalNo, sendQ2);
            logger.info("V-CAT req: {}", req.toString());
            getVcatInterface().executeService(req.toString(), new SmartroVCatCallback.Stub() {

                @Override
                public void onServiceEvent(String strEventJSON) throws RemoteException {

                }

                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onServiceResult(String strResultJSON) throws RemoteException {
                    logger.info("미처리 부분취소 V-CAT 결과: {}", strResultJSON);
                    mainHandler.post(() ->
                            handlePendingPartCancelResult(
                                    strResultJSON, d, cfg, connectorId, sendQ2, callback, dtc));
                }
            });
        } catch (Exception e) {
            logger.error("requestPendingPartCancel error: {}", e.getMessage());
            mainHandler.post(() -> callback.onFailure("부분취소 요청 오류: " + e.getMessage()));
        }
    }

    /**
     * 결과 처리
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void handlePendingPartCancelResult(String strResultJSON,
                                               ChargingCurrentData d,
                                               ChargerConfiguration cfg,
                                               int connectorId,
                                               boolean sendQ2,
                                               PaymentCallback callback,
                                               DateTimeConvert dtc) {
        boolean cancelSuccess = false;
        try {
            JSONObject result    = new JSONObject(strResultJSON);
            String serviceResult = result.optString("service-result", "");
            String responseCode  = result.optString("response-code",  "");
            if ("0000".equals(serviceResult) && "00".equals(responseCode)) {
                // ── 성공 ──────────────────────────────────
                cancelSuccess = true;
                d.setPartCancelTid(    result.optString("van-tran-seq", ""));
                d.setPartCancelNumber( result.optString("approval-no",  ""));
                d.setPartCancelAmount( d.getUsePoint());
                String cancelDt = dtc.dateTimeCovertZoneDate(
                        dtc.convertY4ToY2(result.optString("approval-date", ""))
                                + result.optString("approval-time", ""));
                d.setPartCancelDt(cancelDt);
                logger.info("미처리 부분취소 성공: part_cancel_number={}, amount={}",
                        d.getPartCancelNumber(), d.getPartCancelAmount());
                callback.onSuccess(result);
            } else {
                // ── 실패 ──────────────────────────────────
                String msg = result.optString("display-msg",
                        result.optString("service-description", "부분취소 실패"));
                logger.error("미처리 부분취소 실패: service-result={}, response-code={}, msg={}",
                        serviceResult, responseCode, msg);
                callback.onFailure(msg);
            }
        } catch (Exception e) {
            logger.error("handlePendingPartCancelResult error: {}", e.getMessage());
            callback.onFailure("결과 처리 오류: " + e.getMessage());
        } finally {
            // sendQ2=true 인 경우 (q2 경로): 성공/실패 무관 q2.req 재전송
            if (sendQ2) {
                try {
                    logger.info("q2.req 재전송: connectorId={}, cancelSuccess={}",
                            connectorId, cancelSuccess);
                    // TODO
//                    new PartCancelResultReq(connectorId).sendPartCancelResultReq();
                } catch (Exception e) {
                    logger.error("q2.req 재전송 오류: {}", e.getMessage());
                }
            }
            // f2 전송 (부분취소 결과를 CSMS에 알림)
            if (cancelSuccess) {
                try {
                    logger.info("f2.req 전송 (성공): connectorId={}", connectorId);
                    // TODO
//                    new NonMemberPartCancelPaymentReq(connectorId).sendNonMemberPartCancelPaymentReq();
                } catch (Exception e) {
                    logger.error("f2.req 전송 오류: {}", e.getMessage());
                }
            } else {
                // 실패 시: partCancel* 필드를 비워서 transaction_id 만 전달되도록 초기화
                logger.warn("f2.req 전송 (실패): partCancel 필드 초기화 후 전송, connectorId={}", connectorId);
                d.setPartCancelTid("");
                d.setPartCancelNumber("");
                d.setPartCancelAmount(0);
                d.setPartCancelDt("");
                try {
                    // TODO
//                    new NonMemberPartCancelPaymentReq(connectorId).sendNonMemberPartCancelPaymentReq();
                } catch (Exception e) {
                    logger.error("f2.req 전송 오류 (실패 경로): {}", e.getMessage());
                }
            }
        }
    }



    // ─────────────────────────────────────────────────────
    // 충전 완료 후 부분취소 (ChargingFinishFragment 대체 가능)
    // ─────────────────────────────────────────────────────

    /**
     *  충전 완료 후 선결제 잔액 취소
     * @param usedPayment       실제 충전 사용 금액(0이면 무카드 취소)
     * @param d                 ChargingCurrentData
     * @param cfg               ChargerConfiguration
     * @param connectorId       OCPP connectorId
     * @param callback          결과 callback
     */
    public void requestChargeFinishCancel(int usedPayment,
                                          ChargingCurrentData d,
                                          ChargerConfiguration cfg,
                                          int connectorId,
                                          PaymentCallback callback) {
        if (!isConnected()) {
            logger.error("requestChargeFinishCancel: V-CAT 미연결");
            mainHandler.post(() -> callback.onFailure("V-CAT 서비스에 연결되지 않았습니다."));
            return;
        }

        try {
            final int rate           = 10;
            final int prePayment     = d.getPrePayment();
            final int partCancelAmt  = d.getPrePayment() - d.getPartialCancelPayment();

            DateTimeConvert dtc = new DateTimeConvert();

            JSONObject req = new JSONObject();
            req.put("type",        "credit");
            req.put("member-type", "pg");
            req.put("pg-tran-seq", d.getPgTranSeq());
            req.put("tip",         "0");
            req.put("cat-id",      cfg.getMID());
            req.put("business-no", cfg.getBizNo());
            req.put("approval-no", d.getApprovalNo());
            req.put("approval-date", dtc.dateTimeCovertY4ToY2(d.getApprovalDate()));

            if (usedPayment == 0) {
                GlobalVariables.partialCancel = false;
                req.put("deal",         "no-card-cancel");
                req.put("total-amount", String.valueOf(prePayment));
                req.put("surtax",       String.valueOf((prePayment * rate) / (100 + rate)));
            } else {
                GlobalVariables.partialCancel = true;
                req.put("deal",         "partial-cancel");
                req.put("total-amount", String.valueOf(partCancelAmt));
                req.put("surtax",       String.valueOf((partCancelAmt * rate) / (100 + rate)));
            }
            logger.info("충전 완료 취소 V-CAT 요청: deal={}, amount={}",
                    req.optString("deal"), req.optString("total-amount"));

            vcatInterface.executeService(req.toString(), new SmartroVCatCallback.Stub() {
                @Override
                public void onServiceEvent(String strEventJSON) throws RemoteException {

                }

                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onServiceResult(String strResultJSON) throws RemoteException {
                    mainHandler.post(() ->
                            handleChargeFinishCancelResult(strResultJSON, d, cfg, connectorId, callback));
                }
            });
        } catch (Exception e) {
            logger.error("requestChargeFinishCancel error: {}", e.getMessage());
            mainHandler.post(() -> callback.onFailure("취소 요청 오류: " + e.getMessage()));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void handleChargeFinishCancelResult(String strResultJSON,
                                                ChargingCurrentData d,
                                                ChargerConfiguration cfg,
                                                int connectorId,
                                                PaymentCallback callback) {
        boolean success = false;
        try {
            JSONObject result = new JSONObject(strResultJSON);
            String serviceResult = result.optString("service-result", "");
            String responseCode  = result.optString("response-code",  "");

            if ("0000".equals(serviceResult) && "00".equals(responseCode)) {
                success = true;
                DateTimeConvert dtc = new DateTimeConvert();

                d.setPartCancelTid(    result.optString("van-tran-seq", ""));
                d.setPartCancelNumber( result.optString("approval-no",  ""));
                d.setPartCancelAmount( d.getPartialCancelPayment());

                String cancelDt = dtc.dateTimeCovertZoneDate(
                        dtc.convertY4ToY2(result.optString("approval-date", ""))
                                + result.optString("approval-time", ""));
                d.setPartCancelDt(cancelDt);

                logger.info("충전완료 취소 성공: amount={}", d.getPartCancelAmount());
                callback.onSuccess(result);
            } else {
                String msg = result.optString("display-msg",
                        result.optString("service-description", "취소 실패"));
                logger.error("충전완료 취소 실패: {}", msg);
                callback.onFailure(msg);
            }
        } catch (Exception e) {
            logger.error("handleChargeFinishCancelResult error: {}", e.getMessage());
            callback.onFailure("결과 처리 오류: " + e.getMessage());
        } finally {
            // f2 전송
            try {
                // TODO
//                new NonMemberPartCancelPaymentReq(connectorId).sendNonMemberPartCancelPaymentReq();
            } catch (Exception e) {
                logger.error("f2.req 전송 오류 : {}", e.getMessage());
            }
        }
    }

    private String extractYYmmdd(DateTimeConvert dtc, String iso8601, String fallback) {
        try {
            if (iso8601 != null && iso8601.length() >= 10) {
                // "2024-05-24T..." → "240524"
                String ymd = iso8601.substring(0, 10).replace("-", "");  // "20240524"
                return ymd.substring(2); // "240524"
            }
        } catch (Exception e) {
            logger.warn("extractYYmmdd fallback: {}", e.getMessage());
        }
        // fallback: approvalDate (이미 YYmmdd 또는 YYYYMMDD 형태)
        if (fallback != null && fallback.length() >= 6) {
            return fallback.length() == 8 ? fallback.substring(2) : fallback;
        }
        return "";
    }
}
