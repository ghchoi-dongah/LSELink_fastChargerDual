package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.UserSetSocData;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.UserSetSocRequest;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserSetSocReq {
    private static final Logger logger = LoggerFactory.getLogger(UserSetSocReq.class);


    private final int connectorId;
    public int getConnectorId() {
        return connectorId;
    }

    public UserSetSocReq(int connectorId) {
        this.connectorId = connectorId;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendUserSetSoc() {

        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();

            UserSetSocData userSetSocData = createUserSocData();

            UserSetSocRequest userSetSocRequest = new UserSetSocRequest();
            userSetSocRequest.setVendorId(chargerConfiguration.getChargePointVendor());
            userSetSocRequest.setMessageId("userSetSoc");
            Gson gson = new Gson();
            userSetSocRequest.setData(gson.toJson(userSetSocData));

            activity.getSocketReceiveMessage().onSend(
                    getConnectorId(),
                    userSetSocRequest.getActionName(),
                    userSetSocRequest);

        } catch (Exception e) {
            logger.error(" sendUserSetSoc error : {}", e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private UserSetSocData createUserSocData() {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
        ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(getConnectorId()-1);

        UserSetSocData userSetSocData = new UserSetSocData();
        userSetSocData.setChargeBoxSerialNumber(chargerConfiguration.getChargeBoxSerialNumber());
        userSetSocData.setChargePointSerialNumber(chargerConfiguration.getChargerId());
        userSetSocData.setConnectorId(getConnectorId());
        userSetSocData.setIdTag(chargingCurrentData.getIdTag());
        userSetSocData.setTransactionId(chargingCurrentData.getTransactionId());
        int targetSoc = Math.min(chargingCurrentData.getLimitSoc(), chargingCurrentData.getFullrechgsoc());
        if (targetSoc == 0) {
            targetSoc = chargerConfiguration.getTargetSoc();
        }
        userSetSocData.setSetSoc(targetSoc);
        ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();
        userSetSocData.setTimestamp(zonedDateTimeConvert.doGetKstDatetimeAsString());

        return userSetSocData;
    }
}
