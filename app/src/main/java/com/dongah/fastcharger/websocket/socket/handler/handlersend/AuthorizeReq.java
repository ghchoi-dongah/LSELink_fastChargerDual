package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.utils.LogDataSave;
import com.dongah.fastcharger.websocket.ocpp.core.AuthorizeRequest;
import com.dongah.fastcharger.websocket.socket.SocketState;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class AuthorizeReq {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizeReq.class);

    private final int connectorId ;
    public int getConnectorId() {
        return connectorId;
    }

    public AuthorizeReq(int connectorId) {
        this.connectorId = connectorId;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendAuthorize(String idTag) {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            AuthorizeRequest authorizeRequest = new AuthorizeRequest(idTagWithUserType(idTag));

            SocketState socketState = activity.getSocketReceiveMessage().getSocket().getState();
            if (socketState.equals(SocketState.OPEN)) {
                activity.getSocketReceiveMessage().onSend(
                        getConnectorId(),
                        authorizeRequest.getActionName(),
                        authorizeRequest);
            } else {
                // 통신이 안되면 저장
                String uuid = UUID.randomUUID().toString();
                saveFullAuthorize(getConnectorId(), uuid, authorizeRequest);
            }
//            activity.getChargingCurrentData(getConnectorId()-1).setIdTag(idTag);
        } catch (Exception e) {
            logger.error("sendAuthorize error : {}", e.getMessage());
        }
    }

    private void saveFullAuthorize(
            int connectorId,
            String uniqueId,
            AuthorizeRequest req) {
        try {
            JSONArray frame = new JSONArray();

            frame.put(2);
            frame.put(uniqueId);
            frame.put(req.getActionName());

            JSONObject payload = new JSONObject();
            payload.put("idTag", req.getIdTag());

            frame.put(payload);

            LogDataSave logDataSave = new LogDataSave();
            logDataSave.makeDump(connectorId, frame.toString());
        } catch (Exception e) {
            logger.error("saveFullAuthorize error : {}", e.getMessage());
        }
    }

    private String idTagWithUserType(String idTag) {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(getConnectorId()-1);
            int userType = chargingCurrentData.getPaymentType().value();

            /** pay type : MEMBER(1) CREDIT(2) */
            switch (userType) {
                case 1:
                    chargingCurrentData.setIdTag("M" + chargingCurrentData.getIdTag());
                    break;
                case 2:
                    chargingCurrentData.setIdTag("N" + chargingCurrentData.getIdTag());
                    break;
                default:
                    logger.warn("userType none");
                    break;
            }
            return chargingCurrentData.getIdTag();
        } catch (Exception e) {
            logger.error("idTagWithUserType error : {}", e.getMessage());
            return idTag;
        }
    }
}
