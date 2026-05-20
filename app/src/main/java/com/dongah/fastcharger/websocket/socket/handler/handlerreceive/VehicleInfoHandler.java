package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import com.dongah.fastcharger.websocket.ocpp.core.DataTransferStatus;
import com.dongah.fastcharger.websocket.socket.OcppHandler;

import org.json.JSONObject;

public class VehicleInfoHandler implements OcppHandler  {

    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {

        DataTransferStatus status = DataTransferStatus.valueOf(payload.getString("status"));
        String dataStr = payload.getString("data");   // 먼저 String으로 꺼내고
        JSONObject dataObj = new JSONObject(dataStr);

        // 0 : success  1: fail
        int result = dataObj.getInt("result");
    }
}
