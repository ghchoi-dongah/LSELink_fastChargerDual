package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;
import android.os.Environment;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.utils.FileManagement;
import com.dongah.fastcharger.websocket.ocpp.common.OccurenceConstraintException;
import com.dongah.fastcharger.websocket.ocpp.core.AvailabilityStatus;
import com.dongah.fastcharger.websocket.ocpp.core.AvailabilityType;
import com.dongah.fastcharger.websocket.ocpp.core.ChangeAvailabilityConfirmation;
import com.dongah.fastcharger.websocket.ocpp.core.ChargePointStatus;
import com.dongah.fastcharger.websocket.socket.OcppHandler;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.StatusNotificationReq;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

public class ChangeAvailabilityHandler implements OcppHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChangeAvailabilityHandler.class);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            AvailabilityType type = AvailabilityType.valueOf(payload.getString("type"));

            // 충전 중 일시중지/재시작
            if (type == AvailabilityType.Pause || type == AvailabilityType.Restart) {
                boolean isCharging = isAnyChannelCharging(activity);
                AvailabilityStatus status = isCharging ? AvailabilityStatus.Accepted : AvailabilityStatus.Rejected;
                sendChangeAvailabilityResponse(activity, connectorId, messageId, status);
                if (status == AvailabilityStatus.Rejected) return;
                applyPowerLimitToChargingChannels(activity, connectorId, type);
                return;
            }


            // Operative → 충전기 사용 가능
            boolean checkType = type == AvailabilityType.Operative;

            ChargePointStatus status = (type.equals(AvailabilityType.Operative) || type.equals(AvailabilityType.Managecomplete))
                    ? ChargePointStatus.Available : type.equals(AvailabilityType.Inoperative)
                    ? ChargePointStatus.Unavailable : ChargePointStatus.Maintenance;


            // ChargerOperate
            // connectorId == 0 → 전체 업데이트
            if (connectorId == 0) {
                boolean isCharging = isAnyChannelCharging(activity);
                AvailabilityStatus result = resolveAvailabilityStatus(type, isCharging);
                Arrays.fill(GlobalVariables.ChargerOperation, checkType);

                // change availability response
                sendChangeAvailabilityResponse(activity, connectorId, messageId, result);

                for (int i = 0; i < GlobalVariables.maxChannel; i++) {
                    updateStatusAndNotify(activity, i, status);
                }

            } else {
                boolean isCharging = Objects.equals(
                        activity.getClassUiProcess(connectorId-1).getUiSeq(),
                        UiSeq.CHARGING
                );

                AvailabilityStatus result = resolveAvailabilityStatus(type, isCharging);
                GlobalVariables.ChargerOperation[connectorId] = checkType;

                // change availability response
                sendChangeAvailabilityResponse(activity, connectorId, messageId, result);
                updateStatusAndNotify(activity, connectorId - 1, status);
            }

            onChargerOperateSave(checkType);
        } catch (Exception e) {
            logger.error("ChangeAvailabilityHandler error : {}", e.getMessage(), e);
        }
    }

    private void applyPowerLimitToChargingChannels(MainActivity activity, int connectorId, AvailabilityType type) {
        if (connectorId == 0) {
            for (int i = 0; i < GlobalVariables.maxChannel; i++) {
                short powerLimit = (type == AvailabilityType.Pause) ? (short) 0 : activity.getChargingCurrentData(connectorId-1).getLimitPower();
                applyPowerLimitIfCharging(activity, i, powerLimit);
            }
        } else {
            short powerLimit = (type == AvailabilityType.Pause) ? (short) 0 : activity.getChargingCurrentData(connectorId-1).getLimitPower();
            applyPowerLimitIfCharging(activity, connectorId - 1, powerLimit);
        }
    }

    private void applyPowerLimitIfCharging(MainActivity activity, int channel, short powerLimit) {
        boolean isCharging = Objects.equals(activity.getClassUiProcess(channel).getUiSeq(), UiSeq.CHARGING);
        if (isCharging) {
            activity.getControlBoard().getTxData(channel).setOutPowerLimit(powerLimit);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendChangeAvailabilityResponse(MainActivity activity, int connectorId, String messageId, AvailabilityStatus status) throws OccurenceConstraintException {
        ChangeAvailabilityConfirmation confirmation = new ChangeAvailabilityConfirmation(status);
        activity.getSocketReceiveMessage().onResultSend(
                connectorId,
                confirmation.getActionName(),
                messageId,
                confirmation);
    }

    private boolean isAnyChannelCharging(MainActivity activity) {
        for (int i = 0; i < GlobalVariables.maxChannel; i++) {
            if (Objects.equals(activity.getClassUiProcess(i).getUiSeq(), UiSeq.CHARGING)) {
                return true;
            }
        }
        return false;
    }

    private AvailabilityStatus resolveAvailabilityStatus(AvailabilityType type, boolean isCharging) {
        boolean isInoperativeOrMaintenance =
                type == AvailabilityType.Inoperative || type == AvailabilityType.Maintenance;
        return (isInoperativeOrMaintenance && isCharging)
                ? AvailabilityStatus.Scheduled
                : AvailabilityStatus.Accepted;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateStatusAndNotify(MainActivity activity, int channelIndex, ChargePointStatus status) {
        ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(channelIndex);
        chargingCurrentData.setChargePointStatus(status);

        StatusNotificationReq statusNotificationReq = new StatusNotificationReq(channelIndex + 1);
        statusNotificationReq.sendStatusNotification(channelIndex + 1, chargingCurrentData.getChargePointStatus());
    }

    private void onChargerOperateSave(boolean checkType) {
        try {
            boolean chk;
            FileManagement fileManagement = new FileManagement();
            String rootPath = GlobalVariables.ROOT_PATH;
            String fileName = "ChargerOperate";
            File file = new File(rootPath + File.separator + fileName);
            if (file.exists()) chk = file.delete();

            for (int i = 0; i < GlobalVariables.maxPlugCount; i++) {
                String statusContent = String.valueOf(GlobalVariables.ChargerOperation[i]);
                fileManagement.stringToFileSave(rootPath, fileName, statusContent, true);
            }
        } catch (Exception e) {
            logger.error("onChargerOperateSave error : {}", e.getMessage(), e);
        }
    }
}
