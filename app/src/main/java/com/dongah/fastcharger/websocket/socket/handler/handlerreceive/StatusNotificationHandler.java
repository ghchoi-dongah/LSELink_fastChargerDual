package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import com.dongah.fastcharger.websocket.socket.OcppHandler;

import org.json.JSONObject;

public class StatusNotificationHandler implements OcppHandler  {

    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        int connectorid = connectorId;
        String uuid = messageId;

    }
}
