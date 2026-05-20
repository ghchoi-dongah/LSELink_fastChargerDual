package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.utils.DataTransformation;
import com.dongah.fastcharger.utils.FileManagement;
import com.dongah.fastcharger.websocket.ocpp.core.ChangeConfigurationConfirmation;
import com.dongah.fastcharger.websocket.ocpp.core.ConfigurationStatus;
import com.dongah.fastcharger.websocket.ocpp.core.Reason;
import com.dongah.fastcharger.websocket.socket.OcppHandler;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

public class ChangeConfigurationHandler implements OcppHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChangeConfigurationHandler.class);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        try {
            boolean result;
            MainActivity activity = (MainActivity) MainActivity.mContext;
            ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
            GlobalVariables.setNotSupportedKey(false);
            String key = payload.has("key") ? payload.getString("key") : "";
            String value = payload.has("value") ? payload.getString("value") : "0";
            //valid check

            if (Objects.equals(key, "MeterValueSampleInterval") && Integer.parseInt(value) == -1) {
                result = false;
            }  else if (Objects.equals(key, "SecurityProfile")) {
                result = Integer.parseInt(GlobalVariables.getSecurityProfile()) <= Integer.parseInt(value);
                if (result) setConfigurationValue(key, value);
            } else {
                result = setConfigurationValue(key, value);
            }

            if (result) ((MainActivity) MainActivity.mContext).getConfigurationKeyRead().onRead();

            //response
            ConfigurationStatus configurationStatus = GlobalVariables.isNotSupportedKey() ? ConfigurationStatus.NotSupported :
                    result ? ConfigurationStatus.Accepted : ConfigurationStatus.Rejected;
            ChangeConfigurationConfirmation changeConfigurationConfirmation = new ChangeConfigurationConfirmation(configurationStatus);
            activity.getSocketReceiveMessage().onResultSend(
                    100,
                    changeConfigurationConfirmation.getActionName(),
                    messageId,
                    changeConfigurationConfirmation);


            // websocket 주소 변경
            if (Objects.equals(key, "webSocketURL")) {
                // socket reconnect
                logger.info("webSocketURL changed. Reconnecting to: {}", value);

                // config 변경 및 저장
                activity.getChargerConfiguration().setServerConnectingString(value);
                activity.getChargerConfiguration().onSaveConfiguration();

                // 충전 중이면 종료
                for (int i = 0; i < GlobalVariables.maxChannel; i++) {
                    UiSeq uiSeq = activity.getClassUiProcess(i).getUiSeq();
                    if (UiSeq.CHARGING.equals(uiSeq)) {
                        activity.getControlBoard().getTxData(i).setStart(false);
                        activity.getControlBoard().getTxData(i).setStop(true);
                    }
                }

                // rebooting
                for (int i = 0; i < GlobalVariables.maxChannel; i++) {
                    activity.getChargingCurrentData(i).setStopReason(Reason.HardReset);
                    activity.getChargingCurrentData(i).setReBoot(true);
                }
            } else if (Objects.equals(key, "UseBasicAuth") && Boolean.parseBoolean(value)) {
                // 충전 중이면 종료
                for (int i = 0; i < GlobalVariables.maxChannel; i++) {
                    UiSeq uiSeq = activity.getClassUiProcess(i).getUiSeq();
                    if (UiSeq.CHARGING.equals(uiSeq)) {
                        activity.getControlBoard().getTxData(i).setStart(false);
                        activity.getControlBoard().getTxData(i).setStop(true);
                    }
                }

                // rebooting
                for (int i = 0; i < GlobalVariables.maxChannel; i++) {
                    activity.getChargingCurrentData(i).setStopReason(Reason.HardReset);
                    activity.getChargingCurrentData(i).setReBoot(true);
                }
            }
        } catch (Exception e) {
            logger.error("ChangeConfigurationHandler error :  {}", e.getMessage());
        }
    }

    public boolean setConfigurationValue(String key, String value) {
        boolean result = false;
        try {
            FileManagement fileManagement = new FileManagement();
            String configurationString = fileManagement.getStringFromFile(GlobalVariables.getRootPath() + File.separator + "ConfigurationKey");

            JSONArray jsonArrayContent = new JSONObject(configurationString).getJSONArray("values");
            JSONArray jsonArray = new JSONArray();
            boolean notFond = true;
            for (int i = 0; i < jsonArrayContent.length(); i++) {
                JSONObject contDetail = jsonArrayContent.getJSONObject(i);
                if (Objects.equals(contDetail.get("key"), key)) {
                    if (contDetail.getBoolean("readonly")) {
                        notFond = true;
                    } else {
                        JSONObject obj = new JSONObject();
                        obj.put("key", key);
                        obj.put("readonly", contDetail.getBoolean("readonly"));
                        obj.put("value", doAuthorizationKeyConvert(key, value));
                        jsonArray.put(obj);
                        notFond = false;
                    }
                } else {
                    jsonArray.put(contDetail);
                }
            }
            if (jsonArray.length() > 0) {
                GlobalVariables.setNotSupportedKey(notFond);
                JSONObject sObject = new JSONObject();
                sObject.put("values", jsonArray);
                result = fileManagement.stringToFileSave(GlobalVariables.getRootPath(), "ConfigurationKey", sObject.toString(), false);
            }
        } catch (Exception e) {
            logger.error("SetConfigurationValue {}", e.getMessage());
        }
        return result;
    }

    private String doAuthorizationKeyConvert(String key, String value) {
        try {
            if (Objects.equals(key, "AuthorizationKey")) {
                DataTransformation dataTransformation = new DataTransformation();
                return dataTransformation.hexToString(value);
            } else {
                return value;
            }
        } catch (Exception e) {
            logger.error(" doAuthorizationKeyConvert error : {}", e.getMessage());
            return "0";
        }
    }
}
