package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import com.dongah.fastcharger.websocket.ocpp.core.DataTransferStatus;
import com.dongah.fastcharger.websocket.socket.OcppHandler;

import org.json.JSONObject;

public class ChargingAlarmHandler implements OcppHandler {
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {

        DataTransferStatus status = DataTransferStatus.valueOf(payload.getString("status"));
        String dataStr = payload.has("data") ? payload.getString("data") : "";

    }
}
