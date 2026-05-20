package com.dongah.fastcharger.websocket.socket.handler.handlersend;


import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.CsErrorCode;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.controlboard.ControlBoard;
import com.dongah.fastcharger.controlboard.RxData;
import com.dongah.fastcharger.websocket.ocpp.core.ChargePointErrorCode;
import com.dongah.fastcharger.websocket.ocpp.core.ChargePointStatus;
import com.dongah.fastcharger.websocket.ocpp.core.StatusNotificationRequest;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Objects;

public class StatusNotificationReq {
    private static final Logger logger = LoggerFactory.getLogger(StatusNotificationReq.class);

    private final int connectorId ;

    public StatusNotificationReq(int connectorId) {
        this.connectorId = connectorId;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendStatusNotification() {
        try {
            int startConnectorId, endConnectorId;
            if (getConnectorId() == 0) {
                startConnectorId = 1;
                endConnectorId = GlobalVariables.maxPlugCount;
            } else {
                startConnectorId = getConnectorId();
                endConnectorId = getConnectorId() + 1;
            }

            //응답 대기 시간을 반영 순차적 보냄
            for (int i = startConnectorId; i < endConnectorId; i++) {
                final int rConnectorId = i;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    sendSingleStatusNotification(rConnectorId);
                }, 2000);
            }
        } catch (Exception e) {
            logger.error("sendStatusNotification error : {}", e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendStatusNotification(int connectorId, ChargePointStatus chargePointStatus) {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();
            ZonedDateTime timestamp = zonedDateTimeConvert.doGetCurrentTime();

            StatusNotificationRequest statusNotificationRequest = new StatusNotificationRequest(timestamp);
            statusNotificationRequest.setConnectorId(connectorId);
            ControlBoard controlBoard = activity.getControlBoard();
            RxData rxData = controlBoard.getRxData(connectorId-1);
            ChargePointErrorCode errorCode = (controlBoard.isDisconnected() ? ChargePointErrorCode.EVCommunicationError :
                    rxData.isCsEmergency() ? ChargePointErrorCode.OtherError : ChargePointErrorCode.NoError);
            statusNotificationRequest.setErrorCode(errorCode);
            statusNotificationRequest.setStatus(chargePointStatus);

            activity.getSocketReceiveMessage().onSend(
                    connectorId,
                    statusNotificationRequest.getActionName(),
                    statusNotificationRequest
            );

            // DataTransfer statusnoti
            StatusNotiReq statusNotiReq = new StatusNotiReq(connectorId);
            statusNotiReq.sendStatusNotification();
        } catch (Exception e) {
            logger.error("sendStatusNotification2  error : {}", e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendSingleStatusNotification(int connectorId) {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
            ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();
            ZonedDateTime timestamp = zonedDateTimeConvert.doGetCurrentTime();
            StatusNotificationRequest statusNotificationRequest = new StatusNotificationRequest(timestamp);

            statusNotificationRequest.setConnectorId(connectorId);
            ControlBoard controlBoard = activity.getControlBoard();
            RxData rxData = controlBoard.getRxData(connectorId-1);
            ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(connectorId-1);

            if (!GlobalVariables.ChargerOperation[connectorId]
                    && Objects.equals(chargingCurrentData.getChargePointStatus(), ChargePointStatus.Finishing)) {
                chargingCurrentData.setChargePointStatus(ChargePointStatus.Unavailable);
            }

            String status = chargingCurrentData.getChargePointStatus().name();
            ChargePointErrorCode errorCode = (controlBoard.isDisconnected() ? ChargePointErrorCode.EVCommunicationError :
                    rxData.isCsEmergency() ? ChargePointErrorCode.OtherError : ChargePointErrorCode.NoError);
            statusNotificationRequest.setErrorCode(errorCode);
            statusNotificationRequest.setStatus(rxData.isCsFault() ? ChargePointStatus.Faulted :
                    !GlobalVariables.ChargerOperation[connectorId]  ? ChargePointStatus.Unavailable :
                            ChargePointStatus.valueOf(status));

            // error code
            if (rxData.isCsFault()) {
                statusNotificationRequest.setVendorId(chargerConfiguration.getChargePointVendor());
                statusNotificationRequest.setVendorErrorCode(String.valueOf(getCsErrorCode(rxData)));
            }

            activity.getSocketReceiveMessage().onSend(
                    connectorId,
                    statusNotificationRequest.getActionName(),
                    statusNotificationRequest
            );

            // DataTransfer statusnoti
            StatusNotiReq statusNotiReq = new StatusNotiReq(connectorId);
            statusNotiReq.sendStatusNotification();
        } catch (Exception e) {
            logger.error("sendSingleStatusNotification error : {}", e.getMessage());
        }
    }

    private int getCsErrorCode(RxData rxData) {
        if (rxData.csEmergency) {
            return CsErrorCode.EMERGENCY.value();
        }

        if (rxData.csPLCComm) {
            return CsErrorCode.PLCCOMM.value();
        }

        if (rxData.csPowerMeterComm) {
            return CsErrorCode.POWERMETERCOMM.value();
        }

        if (rxData.csChargerLeak) {
            return CsErrorCode.CHARGERLEAK.value();
        }

        if (rxData.csCarLeak) {
            return CsErrorCode.CARLEAK.value();
        }

        if (rxData.csOutOVR) {
            return CsErrorCode.OUTOVR.value();
        }

        if (rxData.csOutOCR) {
            return CsErrorCode.OUTOCR.value();
        }

        if (rxData.csCouplerTempSensor) {
            return CsErrorCode.COUPLERTEMPSENSOR.value();
        }

        if (rxData.csCouplerOVT) {
            return CsErrorCode.COUPLEROVT.value();
        }

        return 0;
    }

    public int getConnectorId() {
        return connectorId;
    }
}
