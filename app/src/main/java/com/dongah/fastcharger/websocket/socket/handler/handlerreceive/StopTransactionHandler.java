package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.websocket.ocpp.core.AuthorizationStatus;
import com.dongah.fastcharger.websocket.ocpp.core.ChargePointStatus;
import com.dongah.fastcharger.websocket.ocpp.core.Reason;
import com.dongah.fastcharger.websocket.socket.OcppHandler;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.ChargingAlarmReq;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.StatusNotificationReq;

import org.json.JSONObject;

import java.util.Objects;

public class StopTransactionHandler implements OcppHandler  {

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(connectorId-1);
        final StatusNotificationReq statusNotificationReq;

        JSONObject idTagInfo = payload.getJSONObject("idTagInfo");

        AuthorizationStatus status = AuthorizationStatus.valueOf(idTagInfo.getString("status"));
        String parentIdTag = idTagInfo.has("parentIdTag") ? idTagInfo.getString("parentIdTag") : null;

        if (GlobalVariables.isDumpSending(connectorId)) {
            activity.getSocketReceiveMessage().getSocket()
                    .getDumpDataSend(connectorId).onReceiveStopTransactionConf(connectorId);
            return;
        }

        //accept continue
        if (Objects.equals(status, AuthorizationStatus.Accepted)) {

            statusNotificationReq = new StatusNotificationReq(connectorId);
            chargingCurrentData.setChargePointStatus(ChargePointStatus.Finishing);
            statusNotificationReq.sendStatusNotification(connectorId, ChargePointStatus.Finishing);

            // DataTransfer ChargingAlarm
            ChargingAlarmReq chargingAlarmReq = new ChargingAlarmReq(connectorId);
            chargingAlarmReq.sendChargingAlarmReq(3);

            // EVDisconnected 인 경우
            if (Objects.equals(chargingCurrentData.getStopReason(), Reason.EVDisconnected)) {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.postDelayed(() -> {
                    chargingCurrentData.setChargePointStatus(ChargePointStatus.Available);
                    statusNotificationReq.sendStatusNotification(connectorId, ChargePointStatus.Available);
                }, 3000);
            }
        }
    }
}
