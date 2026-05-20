package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.utils.FileManagement;
import com.dongah.fastcharger.websocket.ocpp.localauthlist.GetLocalListVersionConfirmation;
import com.dongah.fastcharger.websocket.socket.OcppHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class GetLocalListVersionHandler implements OcppHandler  {

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        //Local List
        int localListVersion, listVersion;
        FileManagement fileManagement;
        MainActivity activity = (MainActivity) MainActivity.mContext;
        File file = new File(GlobalVariables.getRootPath() + File.separator + "localAuthorizationList");
        if (!file.exists()) {
            listVersion = 0;
        } else {
            fileManagement = new FileManagement();
            JSONObject jsonLocalAuthorizationList = new JSONObject(fileManagement.getStringFromFile(GlobalVariables.getRootPath() + File.separator + "localAuthorizationList"));
            JSONArray localAuthList;
            try {
                localAuthList = jsonLocalAuthorizationList.getJSONArray("localAuthorizationList");
                localListVersion = jsonLocalAuthorizationList.has("listVersion") ? jsonLocalAuthorizationList.getInt("listVersion") : 0;
                listVersion = !GlobalVariables.isLocalAuthListEnabled() ?  -1 : localAuthList.length() == 0 ? 0 : localListVersion;
            } catch (Exception e) {
                listVersion = 0;
            }
        }
        //response
        GetLocalListVersionConfirmation getLocalListVersionConfirmation = new GetLocalListVersionConfirmation(listVersion);
        activity.getSocketReceiveMessage().onResultSend(
                100,
                getLocalListVersionConfirmation.getActionName(),
                messageId,
                getLocalListVersionConfirmation
        );
    }
}
