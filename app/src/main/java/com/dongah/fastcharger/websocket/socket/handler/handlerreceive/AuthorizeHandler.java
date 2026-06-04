package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.FragmentChange;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.utils.ToastPositionMake;
import com.dongah.fastcharger.websocket.ocpp.core.AuthorizationStatus;
import com.dongah.fastcharger.websocket.ocpp.core.ChargePointStatus;
import com.dongah.fastcharger.websocket.socket.OcppHandler;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.DtAuthorizeReq;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.StatusNotificationReq;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.VehicleInfoReq;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

public class AuthorizeHandler implements OcppHandler {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizeHandler.class);
    private final String FILE_NAME = "localList.dongah";


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        FragmentChange fragmentChange = new FragmentChange();
        MainActivity activity = (MainActivity) MainActivity.mContext;
        ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(connectorId-1);
        UiSeq uiSeq = activity.getClassUiProcess(connectorId-1).getUiSeq();
        ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();

        try {
            JSONObject idTagInfo = payload.getJSONObject("idTagInfo");
            AuthorizationStatus status = AuthorizationStatus.valueOf(idTagInfo.getString("status"));
            String parentIdTag = idTagInfo.has("parentIdTag") ? idTagInfo.getString("parentIdTag") : "";
            String expiryDate = idTagInfo.has("expiryDate") ? idTagInfo.getString("expiryDate") : "";

            if (GlobalVariables.isDumpSending(connectorId)) {
                logger.info("Dump Authorize Conf 수신 : {}", idTagInfo);
                activity.getSocketReceiveMessage().getSocket().getDumpDataSend(connectorId).onReceiveAuthorizeConf(connectorId);
                return;
            }

            // 차량 번호
            chargingCurrentData.setParentIdTagStop(parentIdTag);

            if (AuthorizationStatus.Accepted.equals(status)) {
                // AuthorizationCacheEnabled == true ? localList.dongah 저장
                if (GlobalVariables.isAuthorizationCacheEnabled()) {
                    saveIdTagToFile(chargingCurrentData.getIdTag(), idTagInfo);
                }

                if (UiSeq.CHARGING.equals(uiSeq)) {
                    boolean stopConfirm = activity.getChargerConfiguration().isStopConfirm();
                } else {
                    chargingCurrentData.setParentIdTag(parentIdTag);

                    // test mode
                    if (Objects.equals(activity.getChargerConfiguration().getOpMode(), 0)) {
                        chargingCurrentData.setPowerUnitPrice(Double.parseDouble(activity.getChargerConfiguration().getTestPrice()));

                        //test 용
                        chargingCurrentData.setIdTag("C1010010341009611");
                    }

                    // DataTransfer (Authorize)
                    DtAuthorizeReq dtAuthorizeReq = new DtAuthorizeReq(
                            messageId,
                            connectorId,
                            chargingCurrentData.getIdTag()
                    );
                    dtAuthorizeReq.sendDtAuthorize();

                    // DataTransfer vehicleInfo
                    VehicleInfoReq vehicleInfoReq = new VehicleInfoReq(connectorId);
                    vehicleInfoReq.sendVehicleInfo();

                    if (Objects.equals(chargingCurrentData.getChargePointStatus(), ChargePointStatus.Available)) {
                        chargingCurrentData.setChargePointStatus(ChargePointStatus.Preparing);
                        StatusNotificationReq statusNotificationReq = new StatusNotificationReq(connectorId);
                        statusNotificationReq.sendStatusNotification();
                    }

                    activity.getChargingCurrentData(connectorId-1).setAuthorizeResult(true);
                    activity.getClassUiProcess(connectorId-1).setUiSeq(UiSeq.PLUG_CHECK);
                    fragmentChange.onFragmentChange(connectorId-1, UiSeq.PLUG_CHECK, "PLUG_CHECK", null);
                }
            } else {
                String certificationReason = status.name();
                ToastPositionMake toastPositionMake = new ToastPositionMake(activity);
                activity.getChargingCurrentData(connectorId-1).setAuthorizeResult(false);
                if (Objects.equals(uiSeq, UiSeq.CHARGING)) {
                    activity.getClassUiProcess(connectorId-1).setUiSeq(UiSeq.CHARGING);
                    fragmentChange.onFragmentChange(connectorId-1, UiSeq.CHARGING, "CHARGING", null);
                    toastPositionMake.onShowToast(connectorId-1, "충전 중지 인증 실패 : " + certificationReason);
                } else {
                    // 회원 인증 실패
                    activity.getChargingCurrentData(connectorId-1).setAuthorizeResult(false);
                    activity.getClassUiProcess(connectorId-1).setUiSeq(UiSeq.MEMBER_CHECK_FAILED);
                    fragmentChange.onFragmentChange(connectorId-1, UiSeq.MEMBER_CHECK_FAILED, "MEMBER_CHECK_FAILED", null);
                }
            }
        } catch (Exception e) {
            logger.error("AuthorizeHandler error : {}", e.getMessage(), e);
        }
    }

    private void saveIdTagToFile(String idTag, JSONObject idTagInfo) {
        FileWriter fw = null;

        try {
            File file = new File(
                    GlobalVariables.getRootPath()
                            + File.separator
                            + FILE_NAME
            );

            // 부모 폴더 없으면 생성
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            // 1. 파일 존재 여부
            JSONArray listArray;

            if (file.exists() && file.length() > 0) {
                String jsonStr = readFileToString(file);

                if (jsonStr == null || jsonStr.trim().isEmpty()) {
                    listArray = new JSONArray();
                } else {
                    listArray = new JSONArray(jsonStr);
                }
            } else {
                listArray = new JSONArray();
            }

            // 2. 기존 idTag 검색
            boolean found = false;

            for (int i = 0; i < listArray.length(); i++) {
                JSONObject item = listArray.getJSONObject(i);
                String savedIdTag = item.optString("idTag");

                if (savedIdTag.equals(idTag)) {
                    // 있으면 수정
                    item.put("idTagInfo", idTagInfo);
                    found = true;
                    break;
                }
            }

            // 3. 없으면 추가
            if (!found) {
                JSONObject newItem = new JSONObject();
                newItem.put("idTag", idTag);
                newItem.put("idTagInfo", idTagInfo);
                listArray.put(newItem);
            }

            // 4. 파일 저장
            fw = new FileWriter(file, false);
            fw.write(listArray.toString(4));
            fw.flush();

        } catch (Exception e) {
            logger.error("saveIdTagToFile error : ", e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (Exception e) {
                    logger.error("FileWriter close error : ", e);
                }
            }
        }
    }

    private String readFileToString(File file) throws IOException {
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;

        try {
            fis = new FileInputStream(file);
            bos = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int length;

            while ((length = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, length);
            }

            return bos.toString("UTF-8");

        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    logger.error("FileInputStream close error : ", e);
                }
            }

            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    logger.error("ByteArrayOutputStream close error : ", e);
                }
            }
        }
    }
}
