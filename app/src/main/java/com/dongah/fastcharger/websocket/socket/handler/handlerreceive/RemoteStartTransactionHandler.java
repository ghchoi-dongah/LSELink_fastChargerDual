package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.PaymentType;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.websocket.ocpp.core.RemoteStartStopStatus;
import com.dongah.fastcharger.websocket.ocpp.core.RemoteStartTransactionConfirmation;
import com.dongah.fastcharger.websocket.socket.OcppHandler;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.AuthorizeReq;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class RemoteStartTransactionHandler implements OcppHandler  {

    private static final Logger logger = LoggerFactory.getLogger(RemoteStartTransactionHandler.class);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {

        logger.info("RemoteStartTransactionHandler.handle() param connectorId={}, payload connectorId={}, messageId={}, payload={}",
                connectorId,
                payload.optInt("connectorId", -999),
                messageId,
                payload.toString());

        MainActivity activity = ((MainActivity) MainActivity.mContext);

        try {
            int connector = payload.getInt("connectorId");
            ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(connector-1);
            chargingCurrentData.setConnectorId(payload.getInt("connectorId"));
            chargingCurrentData.setIdTag(payload.getString("idTag"));

            // 응답
            sendResponse(connector, messageId);
        } catch (Exception e) {
            logger.error("RemoteStartTransactionHandler handle error : {}", e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendResponse(int connectorId, String messageId) {
        try {
            MainActivity activity = ((MainActivity) MainActivity.mContext);
            UiSeq uiSeq = activity.getClassUiProcess(connectorId-1).getUiSeq();
            ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(connectorId-1);

            RemoteStartStopStatus status = !Objects.equals(uiSeq, UiSeq.INIT) ? RemoteStartStopStatus.Rejected
                    : connectorId == 0 ? RemoteStartStopStatus.Rejected : RemoteStartStopStatus.Accepted;
            RemoteStartTransactionConfirmation remoteStartTransactionConfirmation =
                    new RemoteStartTransactionConfirmation(status);
            activity.getSocketReceiveMessage().onResultSend(
                    connectorId,
                    remoteStartTransactionConfirmation.getActionName(),
                    messageId,
                    remoteStartTransactionConfirmation
            );

            if (Objects.equals(status, RemoteStartStopStatus.Accepted)) {
                String idTag = chargingCurrentData.getIdTag();
                authType(idTag.charAt(0), chargingCurrentData);
                GlobalVariables.RemoteStart[connectorId-1] = true;

                // Authorize
                AuthorizeReq authorizeReq = new AuthorizeReq(connectorId);
                authorizeReq.sendAuthorize(chargingCurrentData.getIdTag());
            }
        } catch (Exception e) {
            logger.error("RemoteStartTransactionHandler sendResponse error : {}", e.getMessage());
        }
    }

    private void authType(char type, ChargingCurrentData chargingCurrentData) {

        try {
            switch (type) {
                case 'C':
                    chargingCurrentData.setAuthType("C");
                    chargingCurrentData.setPaymentType(PaymentType.CORP);
                    chargingCurrentData.setPowerUnitPrice(GlobalVariables.userTypeC);
                    break;
                case 'M':
                    chargingCurrentData.setAuthType("M");
                    chargingCurrentData.setPaymentType(PaymentType.MEMBER);
                    chargingCurrentData.setPowerUnitPrice(GlobalVariables.userTypeM);
                    break;
                case 'N':
                    chargingCurrentData.setAuthType("N");
                    chargingCurrentData.setPaymentType(PaymentType.CREDIT);
                    chargingCurrentData.setPowerUnitPrice(GlobalVariables.userTypeN);
                    break;
                case 'K':
                    chargingCurrentData.setAuthType("K");
                    chargingCurrentData.setPaymentType(PaymentType.MOE);
                    chargingCurrentData.setPowerUnitPrice(GlobalVariables.userTypeK);
                    break;
                default:
                    logger.error("authType none");
                    break;
            }
        } catch (Exception e) {
            logger.error("authType error : {}", e.getMessage(), e);
        }
    }
}
