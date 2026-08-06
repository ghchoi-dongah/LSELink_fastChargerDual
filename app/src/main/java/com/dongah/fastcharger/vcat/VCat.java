package com.dongah.fastcharger.vcat;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.RemoteException;
import android.widget.CheckBox;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import service.vcat.smartro.com.vcat.SmartroVCatCallback;

public class VCat extends ServiceProcessingActivity {
    private static final Logger logger = LoggerFactory.getLogger(VCat.class);

    public VCat(Context context) {
        super(context);
    }
    // ── 서버 프리셋 ────────────────────────────────────────────────────────────
    public static final List<String> SERVER_TEST       = Arrays.asList("eth", "test");
    public static final List<String> SERVER_REAL       = Arrays.asList("eth", "real");
    public static final List<String> SERVER_FORTA_TEST = Arrays.asList("eth", "211.196.246.168", "20101");
    public static final List<String> SERVER_FORTA_REAL = Arrays.asList("eth", "211.196.50.236", "20101");
    public static final List<String> KEY_SERVER_TEST   = Arrays.asList("eth", "test");
    public static final List<String> KEY_SERVER_REAL   = Arrays.asList("eth", "real");

    // ── 거래 타입 ──────────────────────────────────────────────────────────────
    public static final String TYPE_CREDIT   = "credit";
    public static final String TYPE_CASH     = "cash";
    public static final String TYPE_PAY      = "pay";
    public static final String TYPE_BONUS    = "bonus";
    public static final String TYPE_BILL_KEY = "bill-key";

    // ── 거래 구분 ──────────────────────────────────────────────────────────────
    public static final String DEAL_APPROVAL        = "approval";
    public static final String DEAL_CANCELLATION    = "cancellation";
    public static final String DEAL_PARTIAL_CANCEL  = "partial-cancel";
    public static final String DEAL_NO_CARD_CANCEL  = "no-card-cancel";
    public static final String DEAL_REGISTRATION    = "registration";
    public static final String DEAL_DELETE          = "delete";
    public static final String DEAL_INCREASE        = "increase";
    public static final String DEAL_INCREASE_CANCEL = "increase-cancel";
    public static final String DEAL_USE             = "use";
    public static final String DEAL_USE_CANCEL      = "use-cancel";

    // ── 보너스 타입 ────────────────────────────────────────────────────────────
    public static final String BONUS_GS_POINT   = "0";
    public static final String BONUS_OK_CASHBACK = "1";
    public static final String BONUS_L_POINT    = "k";

    // ── 가상 단말기 입력 방식 ──────────────────────────────────────────────────
    public static final String ENTRY_NFC    = "nfc";
    public static final String ENTRY_OCR    = "ocr";
    public static final String ENTRY_KEY_IN = "key-in";

    // ── 가맹점 정보 ────────────────────────────────────────────────────────────
    public String catId      = "1111111111";    // 단말기 번호
    public String businessNo = "1234567890";    // 사업자 번호
    public String memberType = "VAN";           // "VAN" 또는 "PG"
    public String mid = "XXX000000m";           // 가맹 번호(MID)

    // ── 거래 정보 ──────────────────────────────────────────────────────────────
    public String tranType;                     // 거래 타입(credit/cash/bonus)
    public String tranDeal;                     // 거래(approval/cancellation)

    // ── 금액 ───────────────────────────────────────────────────────────────────
    public String totalAmount = "";             // 결제금액(부가세/봉사료가 포함된 최종 금액)
    public String surtax      = "";             // 부가세
    public String tip         = "";             // 봉사료

    // ── 취소용 ────────────────────────────────────────────────────────────────
    public String approvalNo;                   // 승인 번호
    public String approvalDate;                 // 승인 날짜
    public String cashCancelReason;             // 취소 사유: "1"=거래취소, "2"=오발행, "3"=기타
    public String pgTrnSeq = null;

    // ── QR/PAY ────────────────────────────────────────────────────────────────
    public String qrCode = null;                // QR 코드

    // ── 현금영수증 ────────────────────────────────────────────────────────────
    public String cashType = "0";               // "0"=소비자, "1"=사업자

    // ── 보너스 포인트 ─────────────────────────────────────────────────────────
    public String bonusType = "";               // BONUS_GS_POINT / BONUS_OK_CASHBACK / BONUS_L_POINT
    public String bonusPayType = "";            // "0"=카드, "1"=현금(소비자), "2"=현금(사업자)
    public String bonusInputType = "0";         // "0"=카드, "1"=휴대전화번호

    // ── Bill-key ──────────────────────────────────────────────────────────────
    public String billKey;
    public String customerId = "1234567890123456";
    public String customerType = "UM01";

    // ── 기타 ──────────────────────────────────────────────────────────────────
    public String filler1 = "";                  // PGAUTHY / PGAUTHN
    public String filler2 = "";

    // ── 서버 설정 ─────────────────────────────────────────────────────────────
    public List<String> vanComm      = SERVER_TEST;
    public List<String> pgComm       = SERVER_TEST;
    public List<String> securityComm = KEY_SERVER_TEST;

    // ── 가상 단말기 ───────────────────────────────────────────────────────────
    public boolean useAppToApp   = false;
    public String  appToAppEntry;   // ENTRY_NFC / ENTRY_OCR / ENTRY_KEY_IN

    // ── 거래 속성 ─────────────────────────────────────────────────────────────
    public boolean attrContinuousTrx         = false;   // 연속 IC 거래
    public boolean attrIgnoreFallback         = false;   // IC 실패 시 재시도 (Fallback 금지)
    public boolean attrIncludeSignBmpBuffer   = false;   // 응답에 서명 이미지 포함
    public boolean attrEnableSwitchingPayment = false;   // QR/바코드 읽힐 때 PAY 자동 전환
    public boolean attrDisplayChoicePayUi     = false;   // V-CAT 자체 결제수단 선택 UI 사용
    public boolean attrIssuingEreceipt        = false;   // 전자영수증 발행
    public boolean needCardNo                 = false;   // 응답에 카드번호 포함 요청

    // ─────────────────────────────────────────────────────────────────────────
    // JSON 요청 생성
    // ─────────────────────────────────────────────────────────────────────────

    public JSONObject buildRequest() throws JSONException {
        JSONObject json = new JSONObject();

        json.put("device", "app_to_app");
        json.put("app_to_app_entry", ENTRY_NFC);

        json.put("type", tranType);
        json.put("deal", tranDeal);

        switch (tranType) {
            case TYPE_CREDIT:   buildCreditFields(json);  break;
            case TYPE_CASH:     buildCashFields(json);    break;
            case TYPE_PAY:      buildPayFields(json);     break;
            case TYPE_BONUS:    buildBonusFields(json);   break;
            case TYPE_BILL_KEY: buildBillKeyFields(json); break;
        }

        if (!isEmpty(approvalNo))   json.put("approval-no", approvalNo);
        if (!isEmpty(approvalDate)) json.put("approval-date", approvalDate);
        if (!isEmpty(filler1))      json.put("filler1", filler1);
        if (!isEmpty(filler2))      json.put("filler2", filler2);

        if (!isEmpty(surtax))      json.put("surtax", surtax);
        if (!isEmpty(tip))         json.put("tip", tip);
        if (!isEmpty(totalAmount)) json.put("total-amount", totalAmount);
        // 모든 거래는 CAT-ID와 사업자 번호가 필수
        if (!isEmpty(catId))       json.put("cat-id", catId);
        if (!isEmpty(businessNo))  json.put("business-no", businessNo);

        JSONArray attrs = new JSONArray();
        if (attrContinuousTrx)         attrs.put("attr-continuous-trx");
        if (attrIgnoreFallback)         attrs.put("attr-ignore-fallback");
        if (attrIncludeSignBmpBuffer)   attrs.put("attr-include-sign-bmp-buffer");
        if (attrEnableSwitchingPayment) attrs.put("attr-enable-switching-payment");
        if (attrDisplayChoicePayUi)     attrs.put("attr-display-ui-of-choice-pay");
        if (attrIssuingEreceipt)        attrs.put("attr-issuing-ereceipt");
        if (attrs.length() > 0)         json.put("attribute", attrs);

        if (needCardNo) json.put("need-card-no", "y");

        json.put("member-type", memberType);

        if (vanComm != null)      json.put("van-comm", new JSONArray(vanComm));
        if (pgComm != null)       json.put("pg-comm", new JSONArray(pgComm));
        if (securityComm != null) json.put("security-comm", new JSONArray(securityComm));

        if (useAppToApp) {
            json.put("device", "app_to_app");
            if (!isEmpty(appToAppEntry)) json.put("app_to_app_entry", appToAppEntry);
        }

        return json;
    }

    // ── 타입별 필드 빌더 ──────────────────────────────────────────────────────

    private void buildCreditFields(JSONObject json) throws JSONException {
        if (DEAL_PARTIAL_CANCEL.equals(tranDeal) || DEAL_NO_CARD_CANCEL.equals(tranDeal)) {
            if (!isEmpty(pgTrnSeq)) json.put("pg-tran-seq", pgTrnSeq);
        }
    }

    private void buildCashFields(JSONObject json) throws JSONException {
        json.put("cash-type", cashType);
        if (DEAL_CANCELLATION.equals(tranDeal) && !isEmpty(cashCancelReason)) {
            json.put("cancel-reason", cashCancelReason);
        }
    }

    // QR 페이 거래
    private void buildPayFields(JSONObject json) throws JSONException {
        if (!isEmpty(qrCode)) json.put("code", qrCode);
    }

    @SuppressLint("DefaultLocale")
    private void buildBonusFields(JSONObject json) throws JSONException {
        if (!isEmpty(qrCode))       json.put("code", qrCode);
        if (!isEmpty(bonusType))    json.put("bonus-type", bonusType);
        if (!isEmpty(bonusPayType)) json.put("bonus-pay-type", bonusPayType);

        String bonusUseType;
        if (BONUS_OK_CASHBACK.equals(bonusType)) {
            if ("0".equals(bonusPayType))      bonusUseType = "00";
            else if ("1".equals(bonusPayType)) bonusUseType = "01";
            else                               bonusUseType = "11";
        } else {
            bonusUseType = "0".equals(bonusPayType) ? "00" : "01";
        }
        json.put("bonus-use-type", bonusUseType);

        if (BONUS_GS_POINT.equals(bonusType)) {
            StringBuffer buffer = new StringBuffer();

            // 카드매체구분 - 1 : MS, 2 : IC
            buffer.append("2");

            // 결제유형 - W : GS 칼텍스보너스카드(보너스 사용시), C : 신용, J : 제시형할인, T: 상품권, Z : 현금 누적.누적 취소시, W : 나머지
            if ("0".equals(bonusPayType)) buffer.append("C");
            else buffer.append("Z");

            // 과세면세 구분 - 과세 : 1, 면세 : 2
            buffer.append("1");
            // GSC발급 매장 코드 (V10)
            buffer.append("G937510001");
            // 제품코드 (V19)
            buffer.append("0000");
            // 조회구분 : 보너스 조회시만 - 1:주민번호, 2:보너스카드번호
            buffer.append("2");

            json.put("filler1", buffer.toString());
        } else if (BONUS_L_POINT.equals(bonusType)) {
            StringBuffer buffer = new StringBuffer();
            // 구분자
            buffer.append("LPNT");

            // 고객식별구분코드 - '1' : 카드번호, '2' - '고객번호'
            if (bonusInputType.equals("0")) buffer.append("1");
            else buffer.append("2");

            // 거래요청방식구분 - '0' : 맴버스카드제시, '1' - '휴대전화번호', '9' - 기타
            buffer.append(bonusInputType);

            // 포인트 적립 대상 금액
            buffer.append(String.format("%012d", Integer.parseInt(totalAmount)));

            // 현금 매출 금액
            if(bonusPayType.equals("1") || bonusPayType.equals("2"))
                buffer.append(String.format("%012d", Integer.parseInt(totalAmount)));
            else buffer.append("000000000000");

            // 신용카드 매출 금액
            if(bonusPayType.equals("0"))
                buffer.append(String.format("%012d", Integer.parseInt(totalAmount)));
            else
                buffer.append("000000000000");

            // 상품권 매출 금액
            buffer.append("000000000000");
            // 포인트 매출 금액
            buffer.append("000000000000");
            // 기타 매출 금액
            buffer.append("000000000000");
            json.put("filler1", buffer.toString());
        }

        // filler1 자동 생성 (직접 지정하지 않은 경우)
//        if (isEmpty(filler1)) {
//            if (BONUS_GS_POINT.equals(bonusType)) {
//                String payCode = "0".equals(bonusPayType) ? "C" : "Z";
//                filler1 = "2" + payCode + "1G9375100010000002";
//                json.put("filler1", filler1);
//
//            } else if (BONUS_L_POINT.equals(bonusType)) {
//                String idType    = "0".equals(bonusInputType) ? "1" : "2";
//                long   amount    = isEmpty(totalAmount) ? 0L : Long.parseLong(totalAmount);
//                String cashAmt   = ("1".equals(bonusPayType) || "2".equals(bonusPayType))
//                        ? String.format("%012d", amount) : "000000000000";
//                String creditAmt = "0".equals(bonusPayType)
//                        ? String.format("%012d", amount) : "000000000000";
//                filler1 = "LPNT" + idType + bonusInputType
//                        + String.format("%012d", amount)
//                        + cashAmt + creditAmt
//                        + "000000000000000000000000000000000000";
//                json.put("filler1", filler1);
//            }
//        }
    }

    private void buildBillKeyFields(JSONObject json) throws JSONException {
        if (!isEmpty(billKey))      json.put("bill-key", billKey);
        if (!isEmpty(customerType)) json.put("customer-type", customerType);
        if (!isEmpty(customerId))   json.put("customer-id", customerId);
        // 빌키 발급의 경우
        // 카드 확인 후 발급하는 경우 값 넣지 않거나, "PGAUTHY" 입력
        // 카드 확인 없이 발급하는 경우 "PGAUTHN" 입력
        if (tranDeal.equals(DEAL_REGISTRATION)) {
            json.put("filter1", filler1);
        }
    }

    // ── 유틸리티 ──────────────────────────────────────────────────────────────
    private static boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /** 서비스 실행 */
    private void doService() {
        try {
            JSONObject json = new JSONObject();
            json = buildRequest();
            getVCatInterface().executeService(json.toString(), new SmartroVCatCallback.Stub() {

                @Override
                public void onServiceEvent(String strEventJSON) throws RemoteException {
                    try {
                        final JSONObject jsonEvent = new JSONObject(strEventJSON);
                        if (jsonEvent.has("event")) {
                            if (jsonEvent.getString("event").equals("prompt")) {
                                if (jsonEvent.getString("description").equals("card-no")) {
                                    String cardNo = jsonEvent.getString("card-no");
                                    JSONObject jsonPost = new JSONObject();
                                    try {
                                        jsonPost.put("go-next-step", "y");
                                        getVCatInterface().postExtraData(jsonPost.toString());
                                    } catch (Exception e) {
                                        logger.error("error : {}", e.getMessage(), e);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {

                    }
                }

                @Override
                public void onServiceResult(String strEventJSON) throws RemoteException {
                    try {
                        int iResult;
                        final JSONObject jsonResult = new JSONObject(strEventJSON);
                        iResult = Integer.parseInt(jsonResult.getString("service-result"));
                        if (iResult == 0)
                        {
                            if (jsonResult.getString("response-code").equals("00"))
                            {
                                //승인 일시를 설정 합니다.
                                if (jsonResult.has("approval-date") && jsonResult.has("approval-no"))
                                {
                                    approvalDate = jsonResult.getString("approval-date");
                                    approvalNo = jsonResult.getString("approval-no");
                                }

                                //승인 거래 일때만 사용 합니다.
                                if (tranDeal.equals("approval") && jsonResult.has("pg-tran-seq"))
                                {
                                    pgTrnSeq = jsonResult.getString("pg-tran-seq");
                                }

                                //빌키 거래 일때만 사용 합니다.
                                if (tranType.equals("bill-key") && jsonResult.has("bill-key"))
                                {
                                    billKey = jsonResult.getString("bill-key");
                                }
                            }
                            else {
                                logger.error("거래 거절, 거절 사유[{}]", jsonResult.get("display-msg"));
                            }
                        }
                        else
                        {
                            if (jsonResult.has("service-description")) {
                                logger.error("서비스 실행 오류.. 오류 사유: {}({})", jsonResult.get("service-description"), iResult);
                            }
                            else {
                                logger.error("서비스 실행 오류.. 오류 코드: {}", jsonResult.get("service-result"));
                            }
                        }
                    } catch (Exception e) {
                        logger.error("onServiceResult error : {}", e.getMessage(), e);
                    }
                }
            });
        } catch (Exception e) {
            logger.error("doService error : {}", e.getMessage(), e);
        }
    }
}
