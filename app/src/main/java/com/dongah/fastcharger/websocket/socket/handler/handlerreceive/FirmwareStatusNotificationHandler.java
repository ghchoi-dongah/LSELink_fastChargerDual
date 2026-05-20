package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;
import android.os.Environment;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.utils.FileManagement;
import com.dongah.fastcharger.websocket.ocpp.core.Reason;
import com.dongah.fastcharger.websocket.ocpp.firmware.FirmwareStatus;
import com.dongah.fastcharger.websocket.ocpp.firmware.FirmwareStatusNotificationRequest;
import com.dongah.fastcharger.websocket.socket.OcppHandler;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.StatusNotificationReq;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

public class FirmwareStatusNotificationHandler implements OcppHandler  {
    private static final Logger logger = LoggerFactory.getLogger(FirmwareStatusNotificationHandler.class);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {

        boolean check;
        MainActivity activity = ((MainActivity) MainActivity.mContext);
        ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
        if (Objects.equals(chargerConfiguration.getFirmwareStatus(), FirmwareStatus.Downloading)) {
            // download start
        } else if (Objects.equals(chargerConfiguration.getFirmwareStatus(), FirmwareStatus.Downloaded)) {
            chargerConfiguration.setFirmwareStatus(FirmwareStatus.Installing);
            FirmwareStatusNotificationRequest firmwareStatusNotificationRequest =
                    new FirmwareStatusNotificationRequest(chargerConfiguration.getFirmwareStatus());
            activity.getSocketReceiveMessage().onSend(
                    100,
                    firmwareStatusNotificationRequest.getActionName(),
                    firmwareStatusNotificationRequest);
        } else if (Objects.equals(chargerConfiguration.getFirmwareStatus(), FirmwareStatus.Installing)) {
            // FirmwareStatusNotification file create
            String fileName = "FirmwareStatusNotification";
            FileManagement fileManagement = new FileManagement();
            check = fileManagement.fileCreate(fileName, "Firmware-Installed");

            // installed 전송 후 재부팅 시도
            chargerConfiguration.setFirmwareStatus(FirmwareStatus.Installed);
            FirmwareStatusNotificationRequest firmwareStatusNotificationRequest =
                    new FirmwareStatusNotificationRequest(chargerConfiguration.getFirmwareStatus());
            activity.getSocketReceiveMessage().onSend(
                    100,
                    firmwareStatusNotificationRequest.getActionName(),
                    firmwareStatusNotificationRequest);
        } else if (Objects.equals(chargerConfiguration.getFirmwareStatus(), FirmwareStatus.Installed)) {

            // rebooting
            for (int i = 0; i < GlobalVariables.maxChannel; i++) {
                activity.getChargingCurrentData(i).setStopReason(Reason.HardReset);
                activity.getChargingCurrentData(i).setReBoot(true);
            }

            // update firmware 다운 전에 GlobalVariables.ChargerOperation[] = true ==> Unavailable
//            Arrays.fill(GlobalVariables.ChargerOperation, true);
//            onChargerOperateSave();
//            chargerConfiguration.setFirmwareStatus(FirmwareStatus.Idle);
        } else if (Objects.equals(chargerConfiguration.getFirmwareStatus(), FirmwareStatus.DownloadFailed) ||
                Objects.equals(chargerConfiguration.getFirmwareStatus(), FirmwareStatus.InstallationFailed)) {
            // update firmware 다운 전에 GlobalVariables.ChargerOperation[] = false ==> Unavailable
            Arrays.fill(GlobalVariables.ChargerOperation, true);
            onChargerOperateSave();
            //Status Notification - all
            StatusNotificationReq statusNotificationReq = new StatusNotificationReq(0);
            statusNotificationReq.sendStatusNotification();
        }
    }

    private void onChargerOperateSave() {
        try {
            boolean check;
            String rootPath = Environment.getExternalStorageDirectory().toString() + File.separator + "Download";
            File file = new File(rootPath + File.separator + "ChargerOperate");
            if (file.exists()) check = file.delete();

            FileManagement fileManagement = new FileManagement();
            for (int i = 0; i < GlobalVariables.maxPlugCount; i++) {
                String statusContent = String.valueOf(GlobalVariables.ChargerOperation[i]);
                fileManagement.stringToFileSave(rootPath, "ChargerOperate", statusContent, true);
            }

        } catch (Exception e) {
            logger.error("onChargerOperateSave error : {}", e.getMessage());
        }
    }
}
