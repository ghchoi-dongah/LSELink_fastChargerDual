package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.utils.FileManagement;
import com.dongah.fastcharger.websocket.ocpp.core.DataTransferStatus;
import com.dongah.fastcharger.websocket.socket.OcppHandler;

import org.json.JSONObject;

public class UnitPriceHandler implements OcppHandler  {

    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        DataTransferStatus status = DataTransferStatus.valueOf(payload.getString("status"));
        String dataStr = payload.getString("data");

        if (status.equals(DataTransferStatus.Accepted)) {
            // 저장
            FileManagement fileManagement = new FileManagement();
            fileManagement.stringToFileSave(GlobalVariables.getRootPath(), "unitPrice", dataStr, false);
        }
    }
}
