package com.dongah.fastcharger.pages;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.DateTimeConvert;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.smartro.VCatPaymentManager;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.PaymentReq;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CreditCardFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CreditCardFragment extends Fragment {
    private static final Logger logger = LoggerFactory.getLogger(CreditCardFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    int TIME_MAX = 40;
    TextView txtInputAmt, textViewTagTimer;
    ImageView imageViewCreditCard;
    Animation animation;

    DecimalFormat amountFormatter;
    Handler countHandler;
    Runnable countRunnable;

    MainActivity activity;
    ChargingCurrentData chargingCurrentData;

    private boolean paymentInProgress = false;  // 중복 결제 요청 방지
    private boolean paymentCompleted  = false;  // 타이머/결과 핸들러 중복 실행 방지

    // ── 싱글톤 연결 대기 재시도 ───────────────────────────
    /** VCatPaymentManager 미연결 시 재시도 간격 (ms) */
    private static final int BIND_RETRY_MS  = 100;
    /** 최대 재시도 횟수 (100ms * 30 = 3초) */
    private static final int BIND_WAIT_MAX  = 30;
    private int bindRetryCount = 0; // VCatPaymentManager 연결 대기 재시도 횟수
    private final Handler bindRetryHandler = new Handler(Looper.getMainLooper());

    /** ── 카드 인식 대기 ────────────────────────────────────
     /** true: V-CAT onServiceEvent 에서 card-no 이벤트 수신 → 카드 태깅 확인됨 */
    private boolean cardDetected = false;   // onServiceEvent에서 카드 태깅 감지 여부


    public CreditCardFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CreditCardFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CreditCardFragment newInstance(String param1, String param2) {
        CreditCardFragment fragment = new CreditCardFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
            mChannel = getArguments().getInt(CHANNEL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_credit_card, container, false);
        activity= (MainActivity) MainActivity.mContext;
        chargingCurrentData = activity.getChargingCurrentData(mChannel);

        textViewTagTimer = view.findViewById(R.id.textViewTagTimer);
        txtInputAmt = view.findViewById(R.id.txtInputAmt);
        amountFormatter = new DecimalFormat("###,##0");
        imageViewCreditCard = view.findViewById(R.id.imageViewCreditCard);
        animation = AnimationUtils.loadAnimation(getContext(), R.anim.translate);

        return view;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            textViewTagTimer.setText(TIME_MAX + "초");
            imageViewCreditCard.startAnimation(animation);

            try {
                txtInputAmt.setText(amountFormatter.format(GlobalVariables.FullRechgAmt)); // 완충기준 충전금액
            } catch (Exception e) {
                txtInputAmt.setText(amountFormatter.format(1500));
                logger.error("onViewCreated fullRechgAmt error", e);
            }

            startTimeoutCounter();

            // V-CAT 서비스 바인딩은 부모(ServiceProcessingFragment.onCreate)에서 완료
            // 바인딩 완료 콜백(connectedWithService)에서 결제 요청
            waitForConnectionAndPay();

            // V-CAT 결제 요청 (NFC 카드 태깅 대기)
//            if (!activity.getServiceProcessingActivity().isBound()) {
//                logger.warn("ch={} V-CAT 미연결 - 결제 불가", mChannel);
//            } else {
//                try {
//                    int amount = GlobalVariables.FullRechgAmt;
//                    int tax    = Math.round(amount / 11.0f);
//
//                    chargingCurrentData.setPaymentType(PaymentType.CREDIT);
//                    chargingCurrentData.setPrePayment(amount);
//                    chargingCurrentData.setSurtax(tax);
//                    chargingCurrentData.setTip(0);
//
//                    // V-CAT API V3.13 요청 빌드
//                    VCat vcat        = new VCat(activity);
//                    vcat.tranType    = VCat.TYPE_CREDIT;
//                    vcat.tranDeal    = VCat.DEAL_APPROVAL;
//                    vcat.totalAmount = String.valueOf(amount);
//                    vcat.surtax      = String.valueOf(tax);
//                    vcat.tip         = "0";
//                    vcat.needCardNo  = true;
//                    vcat.mid         = activity.getChargerConfiguration().getMID();
//                    // vanComm: 운영 모드에 따라 실서버/테스트 서버 선택
//                    vcat.vanComm     = (activity.getChargerConfiguration().getOpMode() == 0)
//                                       ? VCat.SERVER_TEST : VCat.SERVER_REAL;
//
//                    boolean sent = activity.getServiceProcessingActivity()
//                            .executeService(vcat.buildRequest().toString());
//                    if (sent) {
//                        activity.getClassUiProcess(mChannel).setUiSeq(UiSeq.CREDIT_CARD_WAIT);
//                        activity.getFragmentChange().onFragmentChange(
//                                mChannel, UiSeq.CREDIT_CARD_WAIT, "CREDIT_CARD_WAIT", null);
//                    } else {
//                        logger.warn("ch={} V-CAT executeService 실패", mChannel);
//                    }
//                } catch (Exception e) {
//                    logger.error("ch={} V-CAT 결제 요청 오류", mChannel, e);
//                }
//            }
        } catch (Exception e) {
            logger.error("onViewCreated error : {}", e.getMessage(), e);
        }
    }

    /**
     * VCatPaymentManager 연결될 때까지 최대 3초(100ms * 30회) 대기 후 결제 시작
     * isConnected()만 true: 초기 설정 중 → 재시도
     * isSetupDone() true: 연결되면 즉시 requestVCatPayment() 호출
     * 3초 초과 시 오류 처리, Toast + goHome()
     */
    private void waitForConnectionAndPay() {
        if (!isAdded() || paymentInProgress || paymentCompleted) return;

        if (VCatPaymentManager.getInstance().isSetupDone()) {
            // VCatPaymentManger 연결 확인
            // 초기 설정까지 완료 → 결제 요청 (카드 대기는 V-CAT 내부에서 처리)
            bindRetryCount = 0;
            requestVCatPayment();
        } else if (VCatPaymentManager.getInstance().isConnected() &&
                bindRetryCount < BIND_WAIT_MAX) {
            // 연결은 됐지만 초기 설정 진행 중 → 재시도
            bindRetryCount++;
            logger.debug("초기 설정 완료 대기 중... ({}/{})", bindRetryCount, BIND_WAIT_MAX);
            bindRetryHandler.postDelayed(this::waitForConnectionAndPay, BIND_RETRY_MS);
        } else if (bindRetryCount < BIND_WAIT_MAX) {
            // 미연결 → 재시도
            bindRetryCount++;
            logger.debug("VCatPaymentManager 연결 대기 중... ({}/{})", bindRetryCount, BIND_WAIT_MAX);
            bindRetryHandler.postDelayed(this::waitForConnectionAndPay, BIND_RETRY_MS);
        } else {
            // 3초 초과 → 오류 처리
            logger.error("VCatPaymentManager 초기 설정 타임아웃 ({}ms 초과)",
                    BIND_RETRY_MS * BIND_WAIT_MAX);
            showToast("결제 서비스에 연결할 수 없습니다. 다시 시도해주세요.");
            goHome();
        }
    }

    // ── 선결제 요청 ───────────────────────────────────────
    /**
     * VCatPaymentManager 를 통한 신용카드 선결제 요청
     *
     * 요청 JSON (requestSalePayment 내부):
     *   service      : "payment"
     *   type         : "credit"
     *   deal         : "approval"
     *   cat-id       : ChargerConfiguration.getMID()
     *   business-no  : ChargerConfiguration.getBizNo()
     *   total-amount : ChargingCurrentData.getPrePayment()
     *   surtax       : VAT 포함가 역산 (prePayment * 10 / 110)
     *   tip          : "0"
     *   member-type  : "pg"
     *   attribute    : ["attr-continuous-trx"]
     */
    private void requestVCatPayment() {
        if (paymentInProgress || paymentCompleted) {
            logger.warn("requestVCatPayment: 이미 결제 진행 중 또는 완료됨");
            return;
        }

        if (!VCatPaymentManager.getInstance().isConnected()) {
            logger.error("requestVCatPayment: V-CAT 미연결");
            showToast("결제 서비스에 연결할 수 없습니다. 다시 시도해주세요.");
            goHome();
            return;
        }

        try {
            ChargerConfiguration cfg = activity.getChargerConfiguration();
            int prePayment = chargingCurrentData.getPrePayment();
            int surtax     = (prePayment * 10) / 110;

            JSONObject jsonRequest = new JSONObject();
            jsonRequest.put("service",      "payment");
            jsonRequest.put("type",         "credit");
            jsonRequest.put("deal",         "approval");
            jsonRequest.put("cat-id",       cfg.getMID());
            jsonRequest.put("business-no",  cfg.getBizNo());
            jsonRequest.put("total-amount", String.valueOf(prePayment));
            jsonRequest.put("surtax",       String.valueOf(surtax));
            jsonRequest.put("tip",          "0");
            jsonRequest.put("member-type",  "pg");

//            JSONArray attr = new JSONArray();
//            attr.put("attr-continuous-trx");
//            jsonRequest.put("attribute", attr);

            paymentInProgress = true;
            cardDetected      = false;

            logger.info("=============================== <REQUEST> ===================================");
            logger.info(jsonRequest.toString());
            logger.info("=============================================================================");

            // VCatPaymentManager 인터페이스로 executeService 호출
            VCatPaymentManager.getInstance().getVcatInterface()
                    .executeService(jsonRequest.toString(),
                            new service.vcat.smartro.com.vcat.SmartroVCatCallback.Stub() {

                                @Override
                                public void onServiceEvent(String strEventJSON) {
                                    try {
                                        JSONObject event = new JSONObject(strEventJSON);
                                        String desc = event.optString("description", "");
                                        if ("card-no".equals(desc)) {
                                            cardDetected = true;
                                            String maskedNo = event.optString("card-no", "");
                                            logger.info("카드 인식됨: {}", maskedNo);
                                            // UI 업데이트가 필요하면 여기서 runOnUiThread 처리
                                        }
                                    } catch (Exception e) {
                                        logger.warn("onServiceEvent 파싱 오류: {}", e.getMessage());
                                    }
                                }

                                @RequiresApi(api = Build.VERSION_CODES.O)
                                @Override
                                public void onServiceResult(String strResultJSON) {
                                    logger.info("=============================== <RESULT> ===================================");
                                    logger.info(strResultJSON);
                                    logger.info("============================================================================");
                                    // Binder 스레드 → 메인 스레드
                                    if (!isAdded() || getActivity() == null) return;
                                    getActivity().runOnUiThread(
                                            () -> handlePaymentResult(strResultJSON));
                                }
                            });

        } catch (Exception e) {
            logger.error("requestVCatPayment error: {}", e.getMessage());
            paymentInProgress = false;
            showToast("결제 요청 중 오류가 발생했습니다.");
            goHome();
        }
    }

    // ── 결제 결과 처리 ────────────────────────────────────
    /**
     * V-CAT 결제 결과 파싱 및 처리 (메인 스레드)
     *
     * 성공 조건: service-result = "0000" AND response-code = "00"
     * service-result == "0000" : 정상 승인
     * response-code == "00" : 정상 승인
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void handlePaymentResult(String strResultJSON) {
        paymentInProgress = false;
        try {
            JSONObject result    = new JSONObject(strResultJSON);
            String serviceResult = result.optString("service-result", "");
            String responseCode  = result.optString("response-code",  "");
            String displayMsg    = result.optString("display-msg",    "");

            chargingCurrentData.setTradeCode(responseCode);
            chargingCurrentData.setTradeMethod(displayMsg);

            // V-CAT 서비스 오류
            if (!"0000".equals(serviceResult)) {
                String errDesc = result.optString("service-description", "서비스 오류");
                logger.error("결제 서비스 오류: code={}, desc={}", serviceResult, errDesc);
                showToast("결제 오류: " + errDesc);
                goHome();
                return;
            }

            // VAN 거절
            if (!"00".equals(responseCode)) {
                logger.warn("결제 거절: response-code={}, msg={}", responseCode, displayMsg);
                showToast(displayMsg.isEmpty() ? "결제가 거절되었습니다." : displayMsg);
                goHome();
                return;
            }

            // 승인 성공
            stopTimeoutCounter();
            paymentCompleted = true;

            DateTimeConvert dtc = new DateTimeConvert();
            ChargerConfiguration cfg = activity.getChargerConfiguration();
            // 1. 기본 승인 정보 저장
            chargingCurrentData.setPrePaymentResult(true);
            chargingCurrentData.setApprovalNo(result.getString("approval-no"));
//            chargingCurrentData.setApprovalDate(
//                    dtc.convertY2ToY4(result.getString("approval-date")));  // YYmmdd → YYYYMMDD
            chargingCurrentData.setApprovalDate(
                    result.getString("approval-date"));  // YYmmdd
            chargingCurrentData.setApprovalTime(
                    result.optString("approval-time", ""));
            chargingCurrentData.setPgTranSeq(
                    result.optString("pg-tran-seq", ""));
            chargingCurrentData.setCreditNo(
                    result.optString("card-no", ""));

            // 2. limitApply 필드 세팅 (d2 전문 전송 및 취소 시 사용)
            chargingCurrentData.setAgency("1");
            chargingCurrentData.setLimitApplyMID(cfg.getMID());

            chargingCurrentData.setLimitApplyTid(chargingCurrentData.getApprovalNo());  // 한도승인거래번호
            chargingCurrentData.setLimitApplySeq(chargingCurrentData.getPgTranSeq());   // 한도승인거래일련번호
            chargingCurrentData.setLimitApplyNumber(chargingCurrentData.getApprovalNo());
            chargingCurrentData.setLimitApplyAmount(chargingCurrentData.getPrePayment());   //선결제

            // limitApplyDt: StartTransaction.conf 에서 transactionId 수신 후 d2 전송 시점에 자동 생성
            // → 여기서는 approvalDate + approvalTime 으로 임시 세팅
            chargingCurrentData.setLimitApplyDt(
                    buildIso8601UTC(chargingCurrentData.getApprovalDate(),
                            chargingCurrentData.getApprovalTime()));

            logger.info("선결제 승인 완료: 승인번호={}, 카드={}, 금액={}, pgTranSeq={}",
                    chargingCurrentData.getApprovalNo(),
                    chargingCurrentData.getCreditNo(),
                    chargingCurrentData.getPrePayment(),
                    chargingCurrentData.getPgTranSeq());

            showToast(displayMsg.isEmpty() ? "결제가 승인되었습니다." : displayMsg);

            // d2 전문 전송: NonMemberLimitPaymentReq (선결제 내역 → CSMS)
            // transactionId 는 StartTransaction.conf 이후 확정되지만,
            // 선결제 승인 직후 즉시 전송하여 CSMS 에 한도승인 내역을 알린다.
            // (transactionId 가 0 인 경우 CSMS 에서 이후 매핑 처리)
//            sendLimitPaymentToServer();

            activity.getClassUiProcess(mChannel).setUiSeq(UiSeq.PLUG_CHECK);
            activity.getFragmentChange().onFragmentChange(mChannel, UiSeq.PLUG_CHECK, "PLUG_CHECK", null);

        } catch (Exception e) {
            logger.error("handlePaymentResult error : {}", e.getMessage());
            showToast("결제 결과 처리 중 오류가 발생했습니다.");
            goHome();
        }
    }

    /**
     * d2 전문: NonMemberLimitPaymentReq 전송
     *
     * 전송 성공 → moveNextPage() (PLUG_CHECK 화면)
     * 전송 실패 → 선결제 전체취소(VCatPaymentManager) → 홈 이동
     *
     * ── 전송 데이터 (ChargingCurrentData 에서 자동 참조) ──────
     *   transaction_id    : transactionId
     *   phone_number      : phoneNumber
     *   agency            : "1" (스마트로)
     *   limit_apply_mid   : limitApplyMID
     *   limit_apply_tid   : limitApplyTid  (승인번호)
     *   limit_apply_seq   : limitApplySeq  (PG 일련번호)
     *   limit_apply_number: limitApplyNumber
     *   limit_apply_amount: limitApplyAmount
     *   limit_apply_dt    : limitApplyDt   (ISO 8601)
     *
     * ── d2 전문 전송 결과 판단 ────────────────────────────────
     * NonMemberLimitPaymentReq.sendNonMemberLimitPaymentReq() 는
     * WebSocket send 를 수행하고, 응답(conf)은 비동기로 수신된다.
     *
     * 여기서 "전송 실패"는 WebSocket send 자체가 예외를 던지는 경우
     * (소켓 미연결, 직렬화 오류 등)를 의미한다.
     * → 이 경우 CSMS 에 선결제 정보가 전달되지 않으므로 전체취소 진행
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendLimitPaymentToServer() {
        try {
            // 전송이 완료되고 서버 응답을 받으면 PLUG CHECK 화면으로 이동
            // NonMemberLimitPaymentHandler에서 처리
            int connectorId = chargingCurrentData.getConnectorId();
//            new NonMemberLimitPaymentReq(connectorId)
//                    .sendNonMemberLimitPaymentReq();
            new PaymentReq(connectorId).sendPayment();
            logger.info("d2 전문 전송 완료: connectorId={}, transactionId={}",
                    connectorId, chargingCurrentData.getTransactionId());
        } catch (Exception e) {
            // 전송 실패 → 선결제 전체취소 후 홈 이동
            logger.error("d2 전문 전송 실패: {} → 선결제 전체취소 진행", e.getMessage());
            showToast("서버 전송 실패: 결제를 취소합니다.");
            requestFullCancelAndGoHome();
        }
    }

    /**
     * d2 전문 전송 실패 시 선결제 전체취소 처리
     *
     * VCatPaymentManager.requestChargeFinishCancel(usedPayment=0)
     *   → deal = "cancellation" (전체취소)
     *   → 성공/실패 모두 홈 이동
     */
    private void requestFullCancelAndGoHome() {
        if (!VCatPaymentManager.getInstance().isConnected()) {
            logger.error("requestFullCancelAndGoHome: V-CAT 미연결 → 바로 홈 이동");
            showToast("결제 취소 불가: 고객운영 센터로 연락하세요!");
            // 선결제 상태 초기화
            chargingCurrentData.setPrePaymentResult(false);
            goHome();
            return;
        }

        logger.warn("선결제 전체취소 시작: approvalNo={}, amount={}",
                chargingCurrentData.getApprovalNo(),
                chargingCurrentData.getPrePayment());

        ChargerConfiguration cfg = activity.getChargerConfiguration();
        int connectorId          = chargingCurrentData.getConnectorId();
        VCatPaymentManager.getInstance().requestChargeFinishCancel(
                0,                      // usedPayment=0 → deal="cancellation" 전체취소
                chargingCurrentData,
                cfg,
                connectorId,
                new VCatPaymentManager.PaymentCallback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        logger.info("선결제 전체취소 성공: 취소승인번호={}",
                                result.optString("approval-no", ""));
                        chargingCurrentData.setPrePaymentResult(false);
                        showToast("결제가 취소되었습니다.");
                        goHome();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        // 취소도 실패한 경우 → 수동 처리 필요, 반드시 로그 기록
                        logger.error("선결제 전체취소 실패: {} → 수동 처리 필요", errorMessage);
                        showToast("결제 취소 실패: 고객운영 센터로 연락하세요!\n승인번호: "
                                + chargingCurrentData.getApprovalNo());
                        goHome();
                    }
                }
        );
    }

    /**
     * approvalDate(YYYYMMDD) + approvalTime(HHmmss) → ISO 8601 (YYYY-MM-DDTHH:mm:ssZ)
     * d2 전문의 limit_apply_dt 필드에 사용
     */
    private String buildIso8601(String date8, String time6) {
        try {
            // date8 = "20240524", time6 = "153045"
            if (date8 != null && date8.length() == 8
                    && time6 != null && time6.length() == 6) {
                return date8.substring(0, 4) + "-"
                        + date8.substring(4, 6) + "-"
                        + date8.substring(6, 8) + "T"
                        + time6.substring(0, 2) + ":"
                        + time6.substring(2, 4) + ":"
                        + time6.substring(4, 6) + "Z";
            }
        } catch (Exception e) {
            logger.warn("buildIso8601 error: {}", e.getMessage());
        }
        return "";
    }

    /**
     * approvalDate(YYYYMMDD) + approvalTime(HHmmss) → ISO 8601 UTC (YYYY-MM-DDTHH:mm:ssZ)
     *
     * V-CAT 결제 승인 시각은 KST(UTC+9) 기준이므로 9시간을 빼서 UTC로 변환
     * d2 전문의 limit_apply_dt 필드에 사용
     *
     * 예) "20240524" + "030045" (KST 03:00:45)
     *   → UTC 2024-05-23T18:00:45Z  (날짜 변경선 처리 포함)
     */
    @SuppressLint("DefaultLocale")
    private String buildIso8601UTC(String date8, String time6) {
        try {
            if (date8 == null || date8.length() != 8
                    || time6 == null || time6.length() != 6) {
                return "";
            }

            int year   = Integer.parseInt(date8.substring(0, 4));
            int month  = Integer.parseInt(date8.substring(4, 6));
            int day    = Integer.parseInt(date8.substring(6, 8));
            int hour   = Integer.parseInt(time6.substring(0, 2));
            int minute = Integer.parseInt(time6.substring(2, 4));
            int second = Integer.parseInt(time6.substring(4, 6));

            // KST → UTC: 9시간 차감
            hour -= 9;

            // 날짜 변경 처리 (hour < 0 이면 전날로)
            if (hour < 0) {
                hour += 24;
                day  -= 1;
                if (day < 1) {
                    month -= 1;
                    if (month < 1) {
                        month = 12;
                        year  -= 1;
                    }
                    day = lastDayOfMonth(year, month);
                }
            }

            return String.format("%04d-%02d-%02dT%02d:%02d:%02dZ", year, month, day, hour, minute, second);

        } catch (Exception e) {
            logger.warn("buildIso8601UTC error: {}", e.getMessage());
        }
        return "";
    }

    /**
     * 해당 연월의 마지막 날짜 반환 (윤년 포함)
     */
    private int lastDayOfMonth(int year, int month) {
        switch (month) {
            case 1: case 3: case 5: case 7:
            case 8: case 10: case 12:
                return 31;
            case 4: case 6: case 9: case 11:
                return 30;
            case 2:
                // 윤년: 4의 배수 && (100의 배수가 아니거나 400의 배수)
                return ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) ? 29 : 28;
            default:
                return 30;
        }
    }

    // timeout counter
    private void startTimeoutCounter() {
        try {
            countHandler = new Handler();
            countRunnable = new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    if (!isAdded() || paymentCompleted) {
                        stopTimeoutCounter();
                        return;
                    }

                    TIME_MAX--;
                    if (TIME_MAX <= 0) {
                        stopTimeoutCounter();

                        // 결제 진행 중이면 V-CAT cancelService 호출
                        if (paymentInProgress && VCatPaymentManager.getInstance().isConnected()) {
                            try {
                                VCatPaymentManager.getInstance()
                                        .getVcatInterface().cancelService();
                            } catch (Exception e) {
                                logger.error("cancelService error: {}", e.getMessage());
                            }
                        }
                        showToast("카드 인식 시간이 초과되었습니다.");
                        goHome();

//                        if (!chargingCurrentData.isPrePaymentResult()) {
//                            activity.getServiceProcessingActivity().cancelService();
//                        }

//                        activity.getClassUiProcess(mChannel).onHome();
                    } else {
                        countHandler.postDelayed(countRunnable, 1000);
                        textViewTagTimer.setText(TIME_MAX + "초");
                    }
                }
            };
            countHandler.postDelayed(countRunnable, 1000);
        } catch (Exception e) {
            logger.error("startTimeoutCounter error : {}", e.getMessage(), e);
        }
    }

    private void stopTimeoutCounter() {
        if (countHandler != null && countRunnable != null) {
            countHandler.removeCallbacks(countRunnable);
        }
    }

    private void goHome() {
        if (!isAdded() || getActivity() == null) return;
        ((MainActivity) requireActivity()).getClassUiProcess(mChannel).onHome();
    }

    private void showToast(String message) {
        if (!isAdded() || getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            try {
                android.widget.Toast.makeText(requireContext(), message,
                        android.widget.Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                logger.error("showToast error: {}", e.getMessage());
            }
        });
    }

    @Override
    public void onDestroyView() {
        try {
            if (countHandler != null) {
                countHandler.removeCallbacks(countRunnable);
                countHandler.removeCallbacksAndMessages(null);
            }

            if (animation != null) {
                imageViewCreditCard.clearAnimation();
                animation.setAnimationListener(null);
                animation = null;
            }
        } catch (Exception e) {
            logger.error("onDestroyView error : {}", e.getMessage(), e);
        }
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}