package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import com.dongah.fastcharger.websocket.socket.OcppHandler;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DTAuthorizeHandler implements OcppHandler {
    private static final Logger logger = LoggerFactory.getLogger(DTAuthorizeHandler.class);

    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {

    }
}
