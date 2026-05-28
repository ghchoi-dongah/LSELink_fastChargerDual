package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.PaymentData;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.PaymentInfoData;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.PaymentRequest;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentReq {
    private static final Logger logger = LoggerFactory.getLogger(PaymentReq.class);
    private final int connectorId;
    public int getConnectorId() { return connectorId; }
    public PaymentReq(int connectorId) {
        this.connectorId = connectorId;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendPayment() {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();

            PaymentData paymentData = createPaymentData();
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setVendorId(chargerConfiguration.getChargePointVendor());
            paymentRequest.setMessageId("payment");
            Gson gson = new Gson();
            paymentRequest.setData(gson.toJson(paymentData));

            activity.getSocketReceiveMessage().onSend(
                    getConnectorId(),
                    paymentRequest.getActionName(),
                    paymentRequest
            );

        } catch (Exception e) {
            logger.error("sendPayment error : {}", e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private PaymentData createPaymentData() {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
        ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(getConnectorId()-1);
        ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();

        PaymentData paymentData = new PaymentData();
        paymentData.setChargeBoxSerialNumber(chargerConfiguration.getChargeBoxSerialNumber());
        paymentData.setChargePointSerialNumber(chargerConfiguration.getChargerId());
        paymentData.setConnectorId(getConnectorId());
        paymentData.setTransactionId(chargingCurrentData.getTransactionId());
        paymentData.setIdTag(chargingCurrentData.getIdTag());
        paymentData.setTimestamp(zonedDateTimeConvert.doGetKstDatetimeAsString());

        PaymentInfoData paymentInfoData = createPaymentInfoData();

        Gson gson = new Gson();
        paymentData.setPaymentInfo(gson.toJson(paymentInfoData));

        return paymentData;
    }

    private PaymentInfoData createPaymentInfoData() {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
        ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(getConnectorId()-1);

        // TODO
        PaymentInfoData paymentInfoData = new PaymentInfoData();
        paymentInfoData.setTid(chargingCurrentData.getPgTranSeq()); // 결제승인관리번호
        paymentInfoData.setTrantype(chargingCurrentData.getTradeCode());    // 요청코드
        paymentInfoData.setErrcode(chargingCurrentData.getResponseCode());  // 에러코드
        paymentInfoData.setCardno(chargingCurrentData.getCreditCardNumber());   // 카드번호
        paymentInfoData.setHalbu(chargingCurrentData.getInstallment());     // 할부개월
        paymentInfoData.setTrandate(chargingCurrentData.getApprovalDate()); // 승인일자
        paymentInfoData.setTrantime(chargingCurrentData.getApprovalTime()); // 승인시간
        paymentInfoData.setAuthno(chargingCurrentData.getApprovalNumber()); // 승인번호
        paymentInfoData.setMerno(chargingCurrentData.getStoreNumber());     // 가맹점번호
        // 가맹점일련번호
        paymentInfoData.setStlinst(chargingCurrentData.getIssuer());    // 발급사명
        paymentInfoData.setReqinst(chargingCurrentData.getBuyer());     // 매입사명
        // 서명
        // 승인 메시지
        // 실패내역

        return paymentInfoData;
    }
}
