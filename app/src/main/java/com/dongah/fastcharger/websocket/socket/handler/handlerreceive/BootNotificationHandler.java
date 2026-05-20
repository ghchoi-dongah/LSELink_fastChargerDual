package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.websocket.socket.OcppHandler;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.UnitPriceThread;

import org.json.JSONObject;

import java.util.Objects;

public class BootNotificationHandler implements OcppHandler {

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        String status = payload.getString("status");
        int interval = payload.getInt("interval");
        MainActivity activity = (MainActivity) MainActivity.mContext;
        activity.getProcessHandler().onBootNotificationStop();

        if ("Accepted".equals(status)) {
            GlobalVariables.setConnectRetry(true);
            // Heartbeat 간격 설정 등 기존 로직 이동
            activity.getProcessHandler().onHeartBeatStart(interval);

            // StatusNotification
            // connectorId = 0 : all
            activity.getProcessHandler().onStatusNotificationStart(300);

            //Unit Price
            UnitPriceThread unitPriceThread = new UnitPriceThread(3600);
            unitPriceThread.start();


            if (!Objects.equals(activity.getChargerConfiguration().getOpMode(), 0)) {
                // DataTransfer ChangeMode
                activity.getProcessHandler().onChangeModeStart();

                // DataTransfer ChangeElecMode
                activity.getProcessHandler().onChangeElecModeStart();

                // DataTransfer Rechgrsocschedule
                activity.getProcessHandler().onRechgrsocscheduleStart();
            }

            // dump data send
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
               for (int i = 1; i <= GlobalVariables.maxChannel; i++) {
                   GlobalVariables.setDumpSending(i, true);
                   activity.getSocketReceiveMessage().getSocket().getDumpDataSend(i).onDumpSend(i);
               }
            }, 8000);
        } else {
            activity.getProcessHandler().onBootNotificationStart(5);
            GlobalVariables.setReconnectCheck(false);
        }
    }
}
