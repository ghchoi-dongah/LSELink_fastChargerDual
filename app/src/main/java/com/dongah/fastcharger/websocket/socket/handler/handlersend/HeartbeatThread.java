package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ClassUiProcess;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.utils.LogDataSave;
import com.dongah.fastcharger.websocket.ocpp.common.OccurenceConstraintException;
import com.dongah.fastcharger.websocket.ocpp.core.HeartbeatRequest;
import com.dongah.fastcharger.websocket.socket.SocketReceiveMessage;
import com.dongah.fastcharger.websocket.socket.SocketState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class HeartbeatThread extends Thread{
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatThread.class);

    private volatile boolean stopped = false;
    private final int delayTime;
    private int count = 0;

    private final HeartbeatRequest heartbeatRequest = new HeartbeatRequest();
    private MainActivity activity;
    private SocketReceiveMessage socketReceiveMessage;
    private final LogDataSave logDataSave = new LogDataSave("log");


    @RequiresApi(api = Build.VERSION_CODES.O)
    public HeartbeatThread(int delayTime) {
        this.delayTime = delayTime;

        activity = (MainActivity) MainActivity.mContext;
        if (activity != null) {
            socketReceiveMessage = activity.getSocketReceiveMessage();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    socketReceiveMessage.onSend(
                            100,
                            heartbeatRequest.getActionName(),
                            heartbeatRequest
                    );
                } catch (OccurenceConstraintException e) {
                    throw new RuntimeException(e);
                }
            }, 200);
        }
    }

    public void stopThread() {
        stopped = true;
        interrupt(); // sleep 깨우기
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        logger.info("HeartbeatThread started");
        while (!stopped && !isInterrupted()) {
            try {
                Thread.sleep(java.time.Duration.ofMinutes(delayTime).toMillis());
                processHeartbeat();
            } catch (InterruptedException e) {
                logger.info("HeartbeatThread interrupted");
                Thread.currentThread().interrupt(); // 인터럽트 상태 복구
                break;
            } catch (Exception e) {
                logger.error("HeartbeatThread error : {}", e.getMessage(), e);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void processHeartbeat() throws OccurenceConstraintException {
        if (activity == null) return;

        ClassUiProcess[] classUiProcess = activity.getClassUiProcess();
        boolean sendCheck = true;
        for (int i = 0; i < GlobalVariables.maxChannel; i++) {
            if (classUiProcess[i].getUiSeq() == UiSeq.CHARGING) {
                sendCheck = false;
                break;
            }
        }
        // CHARGING 인 경우 HeartBit 안 보냄.
        if (sendCheck) {
            socketReceiveMessage.onSend(
                    100,
                    heartbeatRequest.getActionName(),
                    heartbeatRequest
            );

        }

        // 30일 이상 로그 삭제
        logDataSave.removeLogData();
        // 미전송 dump 데이터 전송
        SocketState socketState = socketReceiveMessage.getSocket().getState();
        if (socketState == SocketState.OPEN) {
            onDumpData(socketReceiveMessage);
        }
    }

    private void onDumpData(SocketReceiveMessage socketReceiveMessage) {
        for (int connectorId = 1; connectorId <= GlobalVariables.maxChannel; connectorId++) {
            processDumpFile(socketReceiveMessage, connectorId);
        }
    }

    private void processDumpFile(SocketReceiveMessage socketReceiveMessage, int connectorId) {
        File file = new File(GlobalVariables.getRootPath()
                + File.separator + "dump" + File.separator + "dump" + connectorId);

        if (!file.exists()) return;

        java.util.List<String> allLines = new java.util.ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                allLines.add(line);
            }
        } catch (Exception e) {
            logger.error("processDumpFile read error", e);
            return;
        }

        if (allLines.isEmpty()) {
            file.delete();
            return;
        }

        java.util.List<String> remainingLines = new java.util.ArrayList<>();
        int count = 0;
        boolean pauseForStartTx = false;

        for (int i = 0; i < allLines.size(); i++) {
            String line = allLines.get(i);

            if (pauseForStartTx || count >= 9) {
                remainingLines.add(line);
                continue;
            }

            try {
                org.json.JSONArray reqArray = new org.json.JSONArray(line);
                String actionName = reqArray.getString(2);
                org.json.JSONObject payload = reqArray.getJSONObject(3);

                if ("StopTransaction".equals(actionName)) {
                    int currentTxId = payload.optInt("transactionId", 0);
                    int validDumpTxId = GlobalVariables.getDumpTransactionId(connectorId);

                    if (currentTxId <= 0 && validDumpTxId > 0) {
                        payload.put("transactionId", validDumpTxId);
                        reqArray.put(3, payload);
                        line = reqArray.toString();
                    }
                } else if ("DataTransfer".equals(actionName)) {
                    String messageId = payload.optString("messageId", "");
                    if ("MeterValues".equals(messageId) || "chargingAlarm".equals(messageId)) {
                        String dataStr = payload.optString("data", "{}");
                        org.json.JSONObject dataObj = new org.json.JSONObject(dataStr);
                        int currentTxId = dataObj.optInt("transactionId", 0);
                        int validDumpTxId = GlobalVariables.getDumpTransactionId(connectorId);
                        if (currentTxId <= 0 && validDumpTxId > 0) {
                            dataObj.put("transactionId", validDumpTxId);
                            payload.put("data", dataObj.toString());
                            reqArray.put(3, payload);
                            line = reqArray.toString();
                        }
                    }
                }

                socketReceiveMessage.onSend(connectorId, line);
                count++;

                if ("StartTransaction".equals(actionName)) {
                    pauseForStartTx = true;
                }
            } catch (Exception e) {
                logger.error("processDumpFile parse/send error", e);
            }
        }

        if (remainingLines.isEmpty()) {
            file.delete();
        } else {
            try (java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.FileWriter(file))) {
                for (String rLine : remainingLines) {
                    bw.write(rLine);
                    bw.newLine();
                }
            } catch (Exception e) {
                logger.error("processDumpFile rewrite error", e);
            }
        }
    }
}
