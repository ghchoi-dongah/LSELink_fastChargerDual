package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.ModeStatus;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.StatusNotiData;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.StatusNotiRequest;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusNotiReq {
    private static final Logger logger = LoggerFactory.getLogger(StatusNotiReq.class);

    private int connectorId ;

    public StatusNotiReq(int connectorId) {
        this.connectorId = connectorId;
    }

    public int getConnectorId() {
        return connectorId;
    }

    public void setConnectorId() {
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendStatusNotification() {
        try {
            int startConnectorId, endConnectorId;
            if (getConnectorId() == 0) {
                startConnectorId = 1;
                endConnectorId = GlobalVariables.maxPlugCount;
            } else {
                startConnectorId =  getConnectorId();
                endConnectorId = getConnectorId() + 1;
            }

            //응답 대기 시간을 반영 순차적 보냄
            for (int i = startConnectorId; i < endConnectorId; i++) {
                final int rConnectorId = i;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    sendSingleStatusNoti(rConnectorId);
                }, 2000);
            }

        } catch (Exception e) {
            logger.error("sendStatusNotification error :  {}", e.getMessage());
        }
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendSingleStatusNoti(int connectorId) {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
            ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(connectorId-1);

            StatusNotiData statusNotiData = new StatusNotiData();
            statusNotiData.setConnectorId(connectorId);
            statusNotiData.setStatus(ModeStatus.valueOf(chargingCurrentData.getChangeMode()));

            StatusNotiRequest statusNotiRequest = new StatusNotiRequest();
            statusNotiRequest.setVendorId(chargerConfiguration.getChargePointVendor());
            statusNotiRequest.setMessageId("statusnoti");
            Gson gson = new Gson();
            statusNotiRequest.setData(gson.toJson(statusNotiData));

            activity.getSocketReceiveMessage().onSend(connectorId, statusNotiRequest.getActionName(), statusNotiRequest);

        } catch (Exception e) {
            logger.error("sendSingleStatusNoti error : {}", e.getMessage());
        }
    }
}
