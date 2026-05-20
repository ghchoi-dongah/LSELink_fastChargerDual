package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.FullRechgSocData;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.FullRechgSocRequest;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FullRechgSocReq {
    private static final Logger logger = LoggerFactory.getLogger(FullRechgSocReq.class);

    private final int connectorId ;
    public int getConnectorId() {
        return connectorId;
    }

    public FullRechgSocReq(int connectorId) {
        this.connectorId = connectorId;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendFullRechSoc() {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
            ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(getConnectorId()-1);

            FullRechgSocData fullRechgSocData = createFullRechgSocData();

            ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();
            fullRechgSocData.setTimestamp(zonedDateTimeConvert.doGetKstDatetimeAsString());

            FullRechgSocRequest fullRechgSocRequest = new FullRechgSocRequest();
            fullRechgSocRequest.setVendorId(chargerConfiguration.getChargePointVendor());
            fullRechgSocRequest.setMessageId("fullrechgsoc.req");
            Gson gson = new Gson();
            fullRechgSocRequest.setData(gson.toJson(fullRechgSocData));

            activity.getSocketReceiveMessage().onSend(
                    getConnectorId(),
                    fullRechgSocRequest.getActionName(),
                    fullRechgSocRequest
            );

        } catch (Exception e) {
            logger.error("sendFullRechSoc error :  {}", e.getMessage());
        }
    }

    private FullRechgSocData createFullRechgSocData() {

        MainActivity activity = (MainActivity) MainActivity.mContext;
        ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
        ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(getConnectorId()-1);

        FullRechgSocData fullRechgSocData = new FullRechgSocData();
        fullRechgSocData.setChargeBoxSerialNumber(chargerConfiguration.getChargeBoxSerialNumber());
        fullRechgSocData.setChargePointSerialNumber(chargerConfiguration.getChargerId());
        fullRechgSocData.setConnectorId(getConnectorId());
        fullRechgSocData.setIdTag(chargingCurrentData.getIdTag());

        return fullRechgSocData;
    }
}
