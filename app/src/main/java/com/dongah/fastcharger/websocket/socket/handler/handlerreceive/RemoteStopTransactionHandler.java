package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.websocket.ocpp.core.RemoteStartStopStatus;
import com.dongah.fastcharger.websocket.ocpp.core.RemoteStopTransactionConfirmation;
import com.dongah.fastcharger.websocket.socket.OcppHandler;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class RemoteStopTransactionHandler implements OcppHandler  {

    private static final Logger logger = LoggerFactory.getLogger(RemoteStopTransactionHandler.class);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {

        int transactionId = payload.has("transactionId") ? payload.getInt("transactionId") : 0;

        // 응답
        sendResponse(connectorId, messageId, transactionId);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendResponse(int connectorId, String messageId, int transactionId) {
        try {
            MainActivity activity = ((MainActivity) MainActivity.mContext);
            ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(connectorId-1);

            RemoteStartStopStatus status = !Objects.equals(chargingCurrentData.getTransactionId(), transactionId) ?
                    RemoteStartStopStatus.Rejected : RemoteStartStopStatus.Accepted;

            RemoteStopTransactionConfirmation remoteStopTransactionConfirmation =
                    new RemoteStopTransactionConfirmation(status);
            activity.getSocketReceiveMessage().onResultSend(
                    connectorId,
                    remoteStopTransactionConfirmation.getActionName(),
                    messageId,
                    remoteStopTransactionConfirmation
            );
        } catch (Exception e) {
            logger.error(" RemoteStopTransaction sendResponse error : {}", e.getMessage());
        }
    }
}
