package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.websocket.ocpp.core.Reason;
import com.dongah.fastcharger.websocket.ocpp.core.ResetConfirmation;
import com.dongah.fastcharger.websocket.ocpp.core.ResetStatus;
import com.dongah.fastcharger.websocket.ocpp.core.ResetType;
import com.dongah.fastcharger.websocket.socket.OcppHandler;

import org.json.JSONObject;

import java.util.Objects;

public class ResetHandler implements OcppHandler {

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {

        MainActivity activity = ((MainActivity) MainActivity.mContext);
        UiSeq uiSeq;

        ResetType type = ResetType.valueOf(payload.getString("type"));
        ResetConfirmation resetConfirmation = new ResetConfirmation(ResetStatus.Accepted);
        activity.getSocketReceiveMessage().onResultSend(
                100,
                resetConfirmation.getActionName(),
                messageId,
                resetConfirmation
        );

        //charging ==> Stop
        for (int i = 0; i < GlobalVariables.maxChannel; i++) {
            uiSeq = activity.getClassUiProcess(i).getUiSeq();
            if (Objects.equals(uiSeq, UiSeq.CHARGING)) {
                activity.getClassUiProcess(i).onResetStop(type);
            }
            activity.getChargingCurrentData(i).setStopReason(type == ResetType.Hard ?
                    Reason.HardReset : Reason.SoftReset);
            activity.getChargingCurrentData(i).setReBoot(true);
        }
    }
}
