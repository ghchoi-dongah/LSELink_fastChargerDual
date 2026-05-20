package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.utils.LogDataSave;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.ChargingAlarmData;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.ChargingAlarmRequest;
import com.dongah.fastcharger.websocket.socket.SocketState;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ChargingAlarmReq {
    private static final Logger logger = LoggerFactory.getLogger(ChargingAlarmReq.class);

    private final int connectorId ;

    public int getConnectorId() {
        return connectorId;
    }

    public ChargingAlarmReq(int connectorId) {
        this.connectorId = connectorId;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendChargingAlarmReq(int msgType) {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            if (activity == null) return;

            ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
            ChargingAlarmData chargingAlarmData = createChargingAlarmData(msgType);

            ChargingAlarmRequest chargingAlarmRequest = new ChargingAlarmRequest();
            chargingAlarmRequest.setVendorId(chargerConfiguration.getChargePointVendor());
            chargingAlarmRequest.setMessageId("chargingAlarm");
            Gson gson = new Gson();
            chargingAlarmRequest.setData(gson.toJson(chargingAlarmData));

            SocketState socketState = activity.getSocketReceiveMessage().getSocket().getState();
            if (socketState.equals(SocketState.OPEN)) {
                activity.getSocketReceiveMessage().onSend(
                        getConnectorId(),
                        chargingAlarmRequest.getActionName(),
                        chargingAlarmRequest);
            } else {
                // 통신이 안되면 저장
                String uuid = UUID.randomUUID().toString();
                saveFullChargingAlarm(getConnectorId(), uuid, chargingAlarmRequest);
            }
        } catch (Exception e) {
            logger.error("sendChargingAlarmReq error : {}", e.getMessage());
        }
    }

    private ChargingAlarmData createChargingAlarmData(int msgType) {

        MainActivity activity = (MainActivity) MainActivity.mContext;
        ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(getConnectorId()-1);

        ChargingAlarmData chargingAlarmData = new ChargingAlarmData();
        chargingAlarmData.setConnectorId(getConnectorId());
        //(1: 충전 시작, 2: 충전률 90% 도달, 3: 충전 종료
        chargingAlarmData.setMsgType(msgType);
        chargingAlarmData.setTransactionId(chargingCurrentData.getTransactionId());
        chargingAlarmData.setIdTag(chargingCurrentData.getIdTag());
        chargingAlarmData.setPhoneNum("");

        return chargingAlarmData;
    }

    private void saveFullChargingAlarm(
            int connectorId,
            String uniqueId,
            ChargingAlarmRequest req) {
        try {
            JSONArray frame = new JSONArray();

            frame.put(2);
            frame.put(uniqueId);
            frame.put(req.getActionName());

            JSONObject payload = new JSONObject();
            payload.put("vendorId", req.getVendorId());
            payload.put("messageId", req.getMessageId());
            payload.put("data", req.getData());

            frame.put(payload);

            LogDataSave logDataSave = new LogDataSave();
            logDataSave.makeDump(connectorId, frame.toString());
        } catch (Exception e) {
            logger.error("saveFullChargingAlarm error : {}", e.getMessage());
        }
    }
}
