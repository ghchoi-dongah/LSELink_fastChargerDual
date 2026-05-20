package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.AuthorizeData;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.AuthorizeRequest;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DtAuthorizeReq {
    private static final Logger logger = LoggerFactory.getLogger(DtAuthorizeReq.class);

    private final String uuid;
    private final int connectorId;
    private final String idTag;

    public DtAuthorizeReq(String uuid, int connectorId, String idTag) {
        this.uuid = uuid;
        this.connectorId = connectorId;
        this.idTag = idTag;
    }

    public String getUuid() {
        return uuid;
    }

    public int getConnectorId() {
        return connectorId;
    }

    public String getIdTag() {
        return idTag;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendDtAuthorize() {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;

            AuthorizeData authorizeData = new AuthorizeData();
            authorizeData.setUuid(getUuid());
            authorizeData.setConnectorId(getConnectorId());
            authorizeData.setIdTag(getIdTag());

            AuthorizeRequest authorizeRequest = new AuthorizeRequest();
            authorizeRequest.setVendorId(activity.getChargerConfiguration().getChargePointVendor());
            authorizeRequest.setMessageId("Authorize");
            Gson gson = new Gson();
            authorizeRequest.setData(gson.toJson(authorizeData));

            activity.getSocketReceiveMessage().onSend(getConnectorId(), authorizeRequest.getActionName(), authorizeRequest);

        } catch (Exception e) {
            Log.e("DtAuthorizeReq", "sendDtAuthorize error : ", e);
            logger.error("sendDtAuthorize error : {}", e.getMessage());
        }
    }


}
