package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;
import android.os.Environment;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.FirmwareDownload;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.utils.FileManagement;
import com.dongah.fastcharger.websocket.ocpp.common.OccurenceConstraintException;
import com.dongah.fastcharger.websocket.ocpp.firmware.FirmwareStatus;
import com.dongah.fastcharger.websocket.ocpp.firmware.FirmwareStatusNotificationRequest;
import com.dongah.fastcharger.websocket.ocpp.firmware.UpdateFirmwareConfirmation;
import com.dongah.fastcharger.websocket.socket.OcppHandler;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.StatusNotificationReq;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UpdateFirmwareHandler implements OcppHandler  {
    private static final Logger logger = LoggerFactory.getLogger(UpdateFirmwareHandler.class);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {

        String location = payload.has("location") ? payload.getString("location") : "";
        int retries = payload.has("retries") ? payload.getInt("retries") : 1;

        MainActivity activity = ((MainActivity) MainActivity.mContext);

        try {
            //응답
            UpdateFirmwareConfirmation updateFirmwareConfirmation = new UpdateFirmwareConfirmation();
            activity.getSocketReceiveMessage().onResultSend(
                    100,
                    updateFirmwareConfirmation.getActionName(),
                    messageId,
                    updateFirmwareConfirmation
            );

            // 1. firmware status : Downloading
            ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
            chargerConfiguration.setFirmwareStatus(FirmwareStatus.Downloading);
            FirmwareStatusNotificationRequest firmwareStatusNotificationRequest =
                    new FirmwareStatusNotificationRequest(FirmwareStatus.Downloading);
            activity.getSocketReceiveMessage().onSend(
                    100,
                    firmwareStatusNotificationRequest.getActionName(),
                    firmwareStatusNotificationRequest
            );

            // update firmware 다운 전에 GlobalVariables.ChargerOperation[] = false ==> Unavailable
            Arrays.fill(GlobalVariables.ChargerOperation, false);
            onChargerOperateSave();

            // Status Notification - all
            StatusNotificationReq statusNotificationReq = new StatusNotificationReq(0);
            statusNotificationReq.sendStatusNotification();

            //https
            String fileName = location.substring(location.lastIndexOf("/") + 1);
            FirmwareDownload firmwareDownload = new FirmwareDownload(
                    location,
                    fileName,
                    retries,
                    new FirmwareDownload.Callback() {
                        @Override
                        public void onSuccess(File file) {
                            try {

                                // 1. 압축 해제 경로 설정
                                String destDir = file.getParent();
                                unzip(file.getAbsolutePath(), destDir);

                                logger.info("Firmware unzip success : {}", destDir);

                                // 2. zip 파일 삭제
                                boolean deleted = file.delete();
                                if (deleted) {
                                    logger.info("ZIP file deleted successfully: {}", file.getAbsolutePath());
                                } else {
                                    logger.warn("ZIP file deletion failed: {}", file.getAbsolutePath());
                                }

                                // 3. 상태 전송
                                sendFirmwareStatus(FirmwareStatus.Downloaded);
                            } catch (Exception e) {
                                logger.error("Unzip failed : {}", e.getMessage());
                                sendFirmwareStatus(FirmwareStatus.DownloadFailed);
                            }
                        }

                        @Override
                        public void onFail(String message) {
                            sendFirmwareStatus(FirmwareStatus.DownloadFailed);
                        }

                        private void sendFirmwareStatus(FirmwareStatus firmwareStatus) {
                            FirmwareStatusNotificationRequest request =
                                    new FirmwareStatusNotificationRequest(firmwareStatus);
                            try {
                                chargerConfiguration.setFirmwareStatus(firmwareStatus);
                                activity.getSocketReceiveMessage().onSend(
                                        100,
                                        request.getActionName(),
                                        request
                                );
                            } catch (OccurenceConstraintException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
            );
            firmwareDownload.start();
        } catch (Exception e) {
            logger.error("UpdateFirmwareHandler handle error : {}", e.getMessage());
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

    public static void unzip(String zipFilePath, String destDir) throws IOException {

        File dir = new File(destDir);
        if (!dir.exists()) dir.mkdirs();

        byte[] buffer = new byte[4096];

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {

            ZipEntry zipEntry;

            while ((zipEntry = zis.getNextEntry()) != null) {

                File newFile = newFile(dir, zipEntry);

                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // 부모 디렉토리 생성
                    File parent = newFile.getParentFile();
                    if (!parent.exists()) parent.mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }

                zis.closeEntry();
            }
        }
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
