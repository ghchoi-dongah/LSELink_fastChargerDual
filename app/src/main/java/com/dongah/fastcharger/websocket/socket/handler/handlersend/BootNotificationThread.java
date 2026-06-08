package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.os.Build;
import android.os.Environment;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.utils.FileManagement;
import com.dongah.fastcharger.websocket.ocpp.common.OccurenceConstraintException;
import com.dongah.fastcharger.websocket.ocpp.core.BootNotificationRequest;
import com.dongah.fastcharger.websocket.ocpp.firmware.FirmwareStatus;
import com.dongah.fastcharger.websocket.ocpp.security.SecurityEventNotificationRequest;
import com.dongah.fastcharger.websocket.ocpp.security.SignedFirmwareStatus;
import com.dongah.fastcharger.websocket.ocpp.security.SignedFirmwareStatusNotificationRequest;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;
import com.dongah.fastcharger.websocket.socket.SocketReceiveMessage;
import com.dongah.fastcharger.websocket.socket.SocketState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.ZonedDateTime;
import java.util.Arrays;

public class BootNotificationThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(BootNotificationThread.class);

    private volatile boolean stopped = false;
    private final int delayTime;
    private int count = 0;


    private ChargerConfiguration chargerConfiguration;
    private SocketReceiveMessage socketReceiveMessage;

    public BootNotificationThread(int delayTime) {
        this.delayTime = delayTime;
    }

    public void stopThread() {
        stopped = true;
        interrupt(); // sleep 깨우기
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        logger.info("BootNotificationThread started");
        while (!stopped && !isInterrupted()) {
            try {
                Thread.sleep(1000);
                count++;
                if (count >= delayTime) {
                    count = 0;
                    processBootNotification();
                }
            } catch (InterruptedException e) {
                logger.info("BootNotificationThread interrupted");
                break;
            } catch (Exception e) {
                logger.error("BootNotificationThread error : {}", e.getMessage());
            }
        }
        logger.info("BootNotificationThread terminated");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void processBootNotification() throws OccurenceConstraintException {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        if (activity == null) return;

        chargerConfiguration = activity.getChargerConfiguration();
        socketReceiveMessage = activity.getSocketReceiveMessage();

        handleFirmwareStatusFile();
        BootNotificationRequest bootNotificationRequest;
        if (socketReceiveMessage.getSocket().getState() == SocketState.OPEN) {
            bootNotificationRequest = new BootNotificationRequest(
                    chargerConfiguration.getChargePointVendor(),
                    chargerConfiguration.getChargerPointModel());
            bootNotificationRequest.setFirmwareVersion(GlobalVariables.FW_VERSION);
            bootNotificationRequest.setImsi(chargerConfiguration.getImsi());
            bootNotificationRequest.setChargeBoxSerialNumber(chargerConfiguration.getChargeBoxSerialNumber());      // 충전소ID
            bootNotificationRequest.setChargePointSerialNumber(chargerConfiguration.getChargerId());                // 충전기시리얼 → 충전기ID
            bootNotificationRequest.setMeterSerialNumber(chargerConfiguration.getMeterSerialNumber());              // 충전기의 주전력량계의 시리얼 번호
            bootNotificationRequest.setMeterType(chargerConfiguration.getMeterType());  // 충전기의 주전력량계의 타입 포함
            bootNotificationRequest.setIccid(chargerConfiguration.getIccid());          // 모뎀 SIM 카드의 IMSI

            socketReceiveMessage.onSend(
                    100,
                    bootNotificationRequest.getActionName(),
                    bootNotificationRequest
            );
        }


    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void handleFirmwareStatusFile() {

        File firmwareFile = new File(GlobalVariables.getRootPath(), "FirmwareStatusNotification");

        if (!firmwareFile.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(firmwareFile))) {
            String line;

            while ((line = br.readLine()) != null) {
                processFirmwareLine(line);
            }
        } catch (Exception e) {
            logger.error("FirmwareStatus file error", e);
        }
        boolean deleted = firmwareFile.delete();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void processFirmwareLine(String line) {
        try {
            String[] resultStatus = line.split("-");


            if ("SignedFirmware".equals(resultStatus[0])) {
                SignedFirmwareStatus status = SignedFirmwareStatus.valueOf(resultStatus[1]);


                if (status == SignedFirmwareStatus.Installed) {
                    ZonedDateTime timestamp = new ZonedDateTimeConvert().doGetCurrentTime();
                    SecurityEventNotificationRequest req =
                            new SecurityEventNotificationRequest("FirmwareUpdated", timestamp);
                    socketReceiveMessage.onSend(100, req.getActionName(), req);
                }

                SignedFirmwareStatusNotificationRequest req =
                        new SignedFirmwareStatusNotificationRequest(status);
                req.setRequestId(Integer.parseInt(getSignedRequestId()));
                socketReceiveMessage.onSend(100, req.getActionName(), req);
                chargerConfiguration.setSignedFirmwareStatus(status);
            } else if ("Firmware".equals(resultStatus[0])) {
                Arrays.fill(GlobalVariables.ChargerOperation, true);
                onChargerOperateSave();
                chargerConfiguration.setFirmwareStatus(FirmwareStatus.Idle);
            }


        } catch (Exception e) {
            logger.error("processFirmwareLine error: {}", line, e);
        }
    }

    private String getSignedRequestId() {

        File file = new File(GlobalVariables.getRootPath(), "SignedRequestId");

        if (!file.exists()) return "0";
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String id = br.readLine();
            return (id != null) ? id : "0";
        } catch (Exception e) {
            logger.error("getSignedRequestId error", e);
        }
        return "0";
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
