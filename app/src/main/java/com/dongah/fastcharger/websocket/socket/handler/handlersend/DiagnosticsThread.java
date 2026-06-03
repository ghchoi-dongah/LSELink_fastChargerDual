package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.os.Build;
import android.os.Environment;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.utils.FileManagement;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;

public class DiagnosticsThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(DiagnosticsThread.class);

    private static final String FILE_PATH = Environment.getExternalStorageDirectory().toString() + "/Download";
    private static final String FILE_NAME = "diagnostics.dongah";
    private volatile boolean stopped = false;
    long delayTime;
    FileManagement fileManagement;
    ZonedDateTimeConvert zonedDateTimeConvert;
    DecimalFormat powerFormatter = new DecimalFormat("######0.00");

    public void stopThread() {
        stopped = true;
        interrupt();
    }

    public DiagnosticsThread(long delayTime) {
        this.delayTime = delayTime;
        fileManagement = new FileManagement();
        zonedDateTimeConvert = new ZonedDateTimeConvert();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        logger.info("DiagnosticsThread start");
        int count = 0;
        while (!stopped && !isInterrupted()) {
            try {
                Thread.sleep(1000);
                count++;

                if (count > delayTime) {
                    try {
                        count = 0;
                        String startTime = zonedDateTimeConvert.doGetKstDatetimeAsString();
                        String powerMeter = powerFormatter.format(((MainActivity) MainActivity.mContext).getControlBoard().getRxData(0).getPowerMeter() * 0.01);
                        JSONArray data = insertData(startTime, powerMeter);
                        JSONObject obj = new JSONObject();
                        obj.put("diagnostics", data);
                        fileManagement.stringToFileSave(FILE_PATH, FILE_NAME, obj.toString(), true);
                    } catch (Exception e) {
                        logger.error(e.getMessage());
                    }
                }
            } catch (InterruptedException e) {
                // interrupt()로 인해 발생한 예외이므로 종료를 위해 루프를 빠져나가도록 유도
                logger.info("DiagnosticsThread is interrupted. Stopping...");
                Thread.currentThread().interrupt(); // Interrupt 플래그를 다시 세팅
                break;
            } catch (Exception e) {
                logger.info("DiagnosticsThread error : {}", e.getMessage(), e);
            }
        }
        logger.info("DiagnosticsThread terminated");
    }

    public JSONArray insertData(String startTime, String powerMeter) {
        try {
            JSONObject jsonObject = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            jsonObject.put("startTime", startTime);
            jsonObject.put("Energy.Active.Export.Register", powerMeter);
            return jsonArray.put(jsonObject);
        } catch (Exception e) {
            logger.error("insertData() : {}", e.getMessage());
        }
        return null;
    }

    @Override
    public void interrupt() {
        super.interrupt();
    }
}
