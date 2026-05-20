package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.websocket.ocpp.core.ClearCacheConfirmation;
import com.dongah.fastcharger.websocket.ocpp.core.ClearCacheStatus;
import com.dongah.fastcharger.websocket.socket.OcppHandler;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ClearCacheHandler implements OcppHandler {
    private static final Logger logger = LoggerFactory.getLogger(ClearCacheHandler.class);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        MainActivity activity = ((MainActivity) MainActivity.mContext);

        try {
            File file = new File(
                    GlobalVariables.getRootPath()
                            + File.separator
                            + "localList.dongah");

            ClearCacheStatus clearCacheStatus;

            if (file.exists()) {
                file.delete();
                clearCacheStatus = ClearCacheStatus.Accepted;
            } else {
                clearCacheStatus = ClearCacheStatus.Rejected;
            }

            // response
            ClearCacheConfirmation clearCacheConfirmation = new ClearCacheConfirmation(clearCacheStatus);
            activity.getSocketReceiveMessage().onResultSend(
                    connectorId,
                    clearCacheConfirmation.getActionName(),
                    messageId,
                    clearCacheConfirmation);
        } catch (Exception e) {
            logger.error("ClearCacheHandler error : {}", e.getMessage(), e);
        }
    }
}
