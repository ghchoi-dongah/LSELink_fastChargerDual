package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.utils.FileManagement;
import com.dongah.fastcharger.websocket.ocpp.localauthlist.SendLocalListConfirmation;
import com.dongah.fastcharger.websocket.ocpp.localauthlist.UpdateStatus;
import com.dongah.fastcharger.websocket.ocpp.localauthlist.UpdateType;
import com.dongah.fastcharger.websocket.socket.OcppHandler;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

public class SendLocalListHandler implements OcppHandler  {
    private static final Logger logger = LoggerFactory.getLogger(SendLocalListHandler.class);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        try {
            boolean status = false, authSupported;
            FileManagement fileManagement;
            //configuration key SupportedFeatureProfiles check
            authSupported = onSupportedFeatureProfiles("LocalAuthListManagement");
            if (authSupported) {
                String resultValue = "none";
                fileManagement = new FileManagement();
                //서버 에서 받은 데이터
                int newListVersion = payload.getInt("listVersion");
                UpdateType updateType = UpdateType.valueOf(payload.getString("updateType"));
                //configurationKey get list version

                //localAuthorizationList file not found
                File file = new File(GlobalVariables.getRootPath() + File.separator + "localAuthorizationList");
                if (!file.exists() || !payload.has("localAuthorizationList")) {
                    fileManagement.stringToFileSave(GlobalVariables.getRootPath(), "localAuthorizationList", payload.toString(), false);
                    GlobalVariables.updateStatus = UpdateStatus.Accepted;
                } else {
                    //old authorization list
                    JSONObject oldLocalAuthorizationList = new JSONObject(fileManagement.getStringFromFile(GlobalVariables.getRootPath() + File.separator + "localAuthorizationList"));
                    // 현재 localAuthorizationList list 버전
                    int fileListVersion = oldLocalAuthorizationList.getInt("listVersion");
                    //configurationKey localAuthorizationList check
                    resultValue = getConfigurationValue("LocalAuthListEnabled");

                    GlobalVariables.updateStatus = Objects.equals(resultValue, "none") || Objects.equals(resultValue, "false") ? UpdateStatus.NotSupported : UpdateStatus.Accepted;

                    //Accepted, Failed, Not supported, versionMismatch
                    //서버로 부터 받은 세로운 리스트
                    JSONArray newList = payload.getJSONArray("localAuthorizationList");
                    if (Objects.equals(GlobalVariables.updateStatus, UpdateStatus.Accepted)) {
                        if (Objects.equals(UpdateType.Full, updateType)) {
                            if (newList.length() == 0) {
                                String localList = "{\"listVersion\":\"0\",\"localAuthorizationList\":\"\"}" ;
                                status = fileManagement.stringToFileSave(GlobalVariables.getRootPath(), "localAuthorizationList", localList, false);
                            } else {
                                status = fileManagement.stringToFileSave(GlobalVariables.getRootPath(), "localAuthorizationList", payload.toString(), false);
                            }
                            GlobalVariables.updateStatus = status ? UpdateStatus.Accepted : UpdateStatus.Failed;
                        } else if (Objects.equals(UpdateType.Differential, updateType)) {
                            //부분변경
                            try {
                                if (fileListVersion < newListVersion) {
                                    oldLocalAuthorizationList.put("listVersion", payload.getInt("listVersion"));
                                    oldLocalAuthorizationList.put("updateType", payload.getString("updateType"));
                                    //localAuthorizationList 병합
                                    JSONArray oldList = oldLocalAuthorizationList.getJSONArray("localAuthorizationList");
                                    for (int i = 0; i < newList.length(); i++) {
                                        oldList.put(newList.getString(i));
                                    }
                                    status = fileManagement.stringToFileSave(GlobalVariables.getRootPath(), "localAuthorizationList", oldLocalAuthorizationList.toString(), false);
                                    GlobalVariables.updateStatus = status ? UpdateStatus.Accepted : UpdateStatus.Failed;
                                } else {
                                    GlobalVariables.updateStatus = UpdateStatus.VersionMismatch;
                                }
                            } catch (Exception e) {
                                logger.error("{}", e.getMessage());
                            }
                        }
                    }
                }
            } else {
                GlobalVariables.updateStatus = UpdateStatus.NotSupported;
            }
            //send
            MainActivity activity = (MainActivity) MainActivity.mContext;
            SendLocalListConfirmation sendLocalListConfirmation = new SendLocalListConfirmation(GlobalVariables.updateStatus);
            activity.getSocketReceiveMessage().onResultSend(
                    100,
                    sendLocalListConfirmation.getActionName(),
                    messageId,
                    sendLocalListConfirmation
            );
        } catch (Exception e) {
            logger.error("SendLocalList error : {}", e.getMessage());
        }
    }

    public String getConfigurationValue(String key) {
        String result = "none";
        try {
            FileManagement fileManagement = new FileManagement();
            String configurationString = fileManagement.getStringFromFile(GlobalVariables.getRootPath() + File.separator + "ConfigurationKey");
            JSONObject jsonObjectData = new JSONObject(configurationString);
            JSONArray jsonArrayContent = jsonObjectData.getJSONArray("values");
            for (int i = 0; i < jsonArrayContent.length(); i++) {
                JSONObject contDetail = jsonArrayContent.getJSONObject(i);
                if (Objects.equals(contDetail.get("key"), key)) {
                    result = contDetail.getString("value");
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
        }
        return result;
    }

    private boolean onSupportedFeatureProfiles(String key) {
        boolean result = false;
        try {
            String[] values = getConfigurationValue("SupportedFeatureProfiles").split(",");
            for (String value : values) {
                if (Objects.equals(key, value)) {
                    result = true;
                    break;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return result;
    }
}
