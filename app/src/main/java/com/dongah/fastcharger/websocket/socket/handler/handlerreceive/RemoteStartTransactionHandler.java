package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.PaymentType;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.websocket.ocpp.core.ChargePointStatus;
import com.dongah.fastcharger.websocket.ocpp.core.RemoteStartStopStatus;
import com.dongah.fastcharger.websocket.ocpp.core.RemoteStartTransactionConfirmation;
import com.dongah.fastcharger.websocket.socket.OcppHandler;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.AuthorizeReq;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.StatusNotificationReq;

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
            chargingCurrentData.setPaymentType(PaymentType.MEMBER);

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
                chargingCurrentData.setAuthType("C");

                // Authorize
                AuthorizeReq authorizeReq = new AuthorizeReq(connectorId);
                authorizeReq.sendAuthorize(chargingCurrentData.getIdTag());

                // StatusNotification
                chargingCurrentData.setChargePointStatus(ChargePointStatus.Preparing);
                StatusNotificationReq statusNotificationReq = new StatusNotificationReq(connectorId);
                statusNotificationReq.sendStatusNotification(connectorId, ChargePointStatus.Preparing);
            }
        } catch (Exception e) {
            logger.error("RemoteStartTransactionHandler sendResponse error : {}", e.getMessage());
        }
    }
}
