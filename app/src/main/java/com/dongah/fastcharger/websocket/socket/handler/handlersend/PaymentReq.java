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
    private static final Gson gson = new Gson();

    public int getConnectorId() { return connectorId; }
    public PaymentReq(int connectorId) {
        this.connectorId = connectorId;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendPayment() {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
            ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(getConnectorId()-1);

            PaymentData paymentData = createPaymentData(chargingCurrentData, chargerConfiguration);
            PaymentRequest paymentRequest = new PaymentRequest();
            paymentRequest.setVendorId(chargerConfiguration.getChargePointVendor());
            paymentRequest.setMessageId("payment");
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
    private PaymentData createPaymentData(ChargingCurrentData chargingCurrentData, ChargerConfiguration chargerConfiguration) {
        ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();

        PaymentData paymentData = new PaymentData();
        paymentData.setChargeBoxSerialNumber(chargerConfiguration.getChargeBoxSerialNumber());  // chargeBoxSerialNumber    : 충전소ID
        paymentData.setChargePointSerialNumber(chargerConfiguration.getChargerId());            // chargePointSerialNumber  : 충전기ID
        paymentData.setConnectorId(getConnectorId());                                           // connectorId
        paymentData.setTransactionId(chargingCurrentData.getTransactionId());                   // transactionId
        paymentData.setIdTag(chargingCurrentData.getIdTag());                                   // idTag
        paymentData.setTimestamp(zonedDateTimeConvert.doGetKstDatetimeAsString());              // timestamp

        PaymentInfoData paymentInfoData = createPaymentInfoData(chargingCurrentData, chargerConfiguration);
        paymentData.setPaymentInfo(gson.toJson(paymentInfoData));

        return paymentData;
    }

    private PaymentInfoData createPaymentInfoData(ChargingCurrentData chargingCurrentData, ChargerConfiguration chargerConfiguration) {
        PaymentInfoData paymentInfoData = new PaymentInfoData();
        paymentInfoData.setTid(chargingCurrentData.getPgTranSeq());                             // tid          : 결제승인관리번호
        paymentInfoData.setTrantype(chargingCurrentData.getTradeCode());                        // trantype     : 요청코드
        paymentInfoData.setErrcode(chargingCurrentData.getResponseCode());                      // errcode      : 에러코드
        paymentInfoData.setCardno(chargingCurrentData.getCreditCardNumber());                   // cardno       : 카드번호
        paymentInfoData.setHalbu(chargingCurrentData.getInstallment());                         // halbu        : 할부개월
        paymentInfoData.setTrandate(chargingCurrentData.getApprovalDate());                     // tamt         : 승인일자
        paymentInfoData.setTrantime(chargingCurrentData.getApprovalTime());                     // trandate     : 승인시간
        paymentInfoData.setAuthno(chargingCurrentData.getApprovalNumber());                     // authno       : 승인번호
        paymentInfoData.setMerno(chargingCurrentData.getStoreNumber());                         // merno        : 가맹점번호
        paymentInfoData.setTranSerial(chargerConfiguration.getBizNo());                         // tran_serial  : 가맹점일련번호
        paymentInfoData.setStlinst(chargingCurrentData.getIssuer());                            // stlinst      : 발급사명
        paymentInfoData.setReqinst(chargingCurrentData.getBuyer());                             // reqinst      : 매입사명

        // TODO
        // signpath     : 서명
        // msg1         : 승인 메시지
        // msg4         : 실패내역

        return paymentInfoData;
    }
}
