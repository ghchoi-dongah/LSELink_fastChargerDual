package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.websocket.ocpp.core.Reason;
import com.dongah.fastcharger.websocket.ocpp.core.RemoteStartStopStatus;
import com.dongah.fastcharger.websocket.ocpp.core.RemoteStopTransactionConfirmation;
import com.dongah.fastcharger.websocket.socket.OcppHandler;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
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

            // RemoteStop을 실행할 transactionId가 있는지 확인
            boolean result = false;
            for (int i = 0; i < GlobalVariables.maxChannel; i++) {
                ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(i);
                UiSeq uiSeq = activity.getClassUiProcess(i).getUiSeq();
                if (Objects.equals(uiSeq, UiSeq.CHARGING) && Objects.equals(chargingCurrentData.getTransactionId(), transactionId)) {
                    activity.getClassUiProcess(i).onRemoteTransactionStop(i, Reason.Remote);
                    GlobalVariables.RemoteStart[i] = false;
                    result = true;
                    break;
                }
            }

            if (result) {
                boolean found = GlobalVariables.remoteConnectorId.containsValue(transactionId);
                int rConnectorId = -1;
                if (found) {
                    // 일치하는 transactionId 있음 → 해당 connectorId도 찾을 수 있음
                    rConnectorId = GlobalVariables.remoteConnectorId.entrySet().stream()
                            .filter(e -> e.getValue() == transactionId)
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(-1);
                }

                if (rConnectorId != -1) {
                    GlobalVariables.remoteConnectorId.remove(rConnectorId);
                }
            }

            RemoteStartStopStatus status = result ? RemoteStartStopStatus.Accepted : RemoteStartStopStatus.Rejected;
            RemoteStopTransactionConfirmation remoteStopTransactionConfirmation =
                    new RemoteStopTransactionConfirmation(status);
            activity.getSocketReceiveMessage().onResultSend(
                    100,
                    remoteStopTransactionConfirmation.getActionName(),
                    messageId,
                    remoteStopTransactionConfirmation
            );

        } catch (Exception e) {
            logger.error("sendResponse error : {}", e.getMessage());
        }
    }
}
