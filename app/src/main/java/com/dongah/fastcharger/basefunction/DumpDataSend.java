package com.dongah.fastcharger.basefunction;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.websocket.ocpp.common.JSONCommunicator;
import com.dongah.fastcharger.websocket.ocpp.common.model.Message;
import com.dongah.fastcharger.websocket.ocpp.core.AuthorizeRequest;
import com.dongah.fastcharger.websocket.ocpp.core.Location;
import com.dongah.fastcharger.websocket.ocpp.core.MeterValue;
import com.dongah.fastcharger.websocket.ocpp.core.Reason;
import com.dongah.fastcharger.websocket.ocpp.core.SampledValue;
import com.dongah.fastcharger.websocket.ocpp.core.StartTransactionRequest;
import com.dongah.fastcharger.websocket.ocpp.core.StopTransactionRequest;
import com.dongah.fastcharger.websocket.ocpp.core.ValueFormat;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.ChargingAlarmData;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.ChargingAlarmRequest;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.MeterValuesRequest;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * 오프라인 상태에서 누적된 dump 파일을 온라인 복귀 후 순서대로 전송한다.
 *
 * 전송 순서 (하나의 DumpTransaction):
 *   Authorize → StartTransaction (→ 서버 응답으로 transactionId 수신)
 *   → ChargingAlarm(msgType=1) → MeterValues(복수) → StopTransaction → ChargingAlarm(msgType=3)
 *
 * StartTransaction이 있는 경우:
 *   응답으로 받은 transactionId 를 이후 전문에 주입 후 전송.
 * StartTransaction이 없는 경우:
 *   이미 유효한 transactionId 포함 → 그대로 전송.
 */
public class DumpDataSend extends JSONCommunicator {

    private static final Logger logger = LoggerFactory.getLogger(DumpDataSend.class);

    Handler handler = new Handler(Looper.getMainLooper());  // 지연 전송, timeout 처리용

    // ── 충전 세션 1건을 나타내는 데이터 클래스 ────────────────────────────────
    static class DumpTransaction {
        String authorizeLine;
        String startLine;
        String alarmStartLine;                                // ChargingAlarm msgType=1
        ArrayList<String> meterValuesLines = new ArrayList<>();
        String stopLine;
        String alarmStopLine;                                 // ChargingAlarm msgType=3
    }

    /** 재전송할 세션들을 담은 큐 */
    Queue<DumpTransaction> dumpQueue = new LinkedList<>();
    DumpTransaction currentTx;  // 현재 재전송 중인 전문
    MainActivity activity;
    String dumpPath;

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * dump 파일을 읽어 DumpTransaction 큐를 구성하고 순서대로 전송한다.
     *
     * @param connectorId 커넥터 ID (1-based)
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onDumpSend(int connectorId) {
        activity = (MainActivity) MainActivity.mContext;

        try {
            dumpPath = GlobalVariables.getRootPath()
                    + File.separator + "dump"
                    + File.separator + "dump" + connectorId;

            File file = new File(dumpPath);
            if (!file.exists()) {
                logger.info("[Dump][{}] 파일 없음 – 전송 생략", connectorId);
                GlobalVariables.setDumpSending(connectorId, false);
                return;
            }

            dumpQueue.clear();  // clear
            currentTx = null;

            loadDumpFile(connectorId, file); // dump data load

            if (dumpQueue.isEmpty()) {
                logger.info("[Dump][{}] 재전송 대상 없음", connectorId);
                if (!file.delete()) {
                    logger.warn("[Dump][{}] 빈 dump 파일 삭제 실패: {}", connectorId, dumpPath);
                }
                GlobalVariables.setDumpSending(connectorId, false);
                return;
            }

            if (!file.delete()) {
                logger.warn("[Dump][{}] dump 원본 삭제 실패: {}", connectorId, dumpPath);
            }

            // dump data send
            logger.info("[Dump][{}] dump 재전송 시작. txCount={}", connectorId, dumpQueue.size());
            for (DumpTransaction tx : dumpQueue) {
                logger.info("[Dump][{}]   tx: auth={}, start={}, alarmStart={}, meterValues={}, stop={}, alarmStop={}",
                        connectorId,
                        tx.authorizeLine != null,
                        tx.startLine != null,
                        tx.alarmStartLine != null,
                        tx.meterValuesLines.size(),
                        tx.stopLine != null,
                        tx.alarmStopLine != null);
            }
            sendNextTransaction(connectorId);
        } catch (Exception e) {
            GlobalVariables.setDumpSending(connectorId, false);
            logger.error("[Dump][{}] onDumpSend error : {}", connectorId, e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void loadDumpFile(int connectorId, File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            DumpTransaction tx = null;

            while ((line = br.readLine()) != null) {
                try {
                    Message msg = parse(line);

                    if (msg == null || msg.getAction() == null) {
                        logger.warn("[Dump][{}] parse 실패 또는 action 없음: {}", connectorId, line);
                        continue;
                    }

                    String action = msg.getAction();

                    if ("Authorize".equals(action)) {
                        if (tx != null) {
                            dumpQueue.add(tx);
                        }
                        tx = new DumpTransaction();
                        tx.authorizeLine = line;
                    }
                    else if ("StartTransaction".equals(action)) {
                        if (tx == null) {
                            tx = new DumpTransaction();
                        }
                        tx.startLine = line;
                    }
                    else if ("StopTransaction".equals(action)) {
                        if (tx == null) {
                            tx = new DumpTransaction();
                        }
                        tx.stopLine = line;
                    }
                    else if ("DataTransfer".equals(action)) {
                        if (tx == null) {
                            tx = new DumpTransaction();
                        }
                        classifyDataTransfer(connectorId, line, tx);
                    }
                    else {
                        // 그 외 action은 현재 요구사항상 transaction 구성에서 제외
                        logger.debug("[Dump][{}] unsupported action skip: {}", connectorId, action);
                    }

                } catch (Exception e) {
                    logger.error("[Dump][{}] dump line parse error: {}", connectorId, line, e);
                }
            }

            if (tx != null) {
                dumpQueue.add(tx);
            }
        }
    }

    private void classifyDataTransfer(int connectorId, String line, DumpTransaction tx) {
        JsonArray arr = JsonParser.parseString(line).getAsJsonArray();
        JsonObject payload = getPayloadObject(arr);
        if (payload == null) {
            logger.warn("[Dump][{}] DataTransfer payload 없음: {}", connectorId, line);
            return;
        }

        String messageId = getAsStringSafe(payload.get("messageId"));

        JsonObject dataObj = null;
        JsonElement dataElement = payload.get("data");
        if (dataElement != null) {
            if (dataElement.isJsonObject()) {
                dataObj = dataElement.getAsJsonObject();
            } else if (dataElement.isJsonPrimitive()) {
                dataObj = JsonParser.parseString(dataElement.getAsString()).getAsJsonObject();
            }
        }

        // ChargingAlarm
        if ("chargingAlarm".equals(messageId)) {
            if (dataObj == null) {
                logger.warn("[Dump][{}] chargingAlarm data 없음", connectorId);
                return;
            }

            Integer msgType = getAsIntSafe(dataObj.get("msgType"));
            if (msgType == null) {
                logger.warn("[Dump][{}] chargingAlarm msgType 없음", connectorId);
                return;
            }

            if (msgType == 1) {
                tx.alarmStartLine = line;
            } else if (msgType == 3) {
                tx.alarmStopLine = line;
            } else {
                logger.debug("[Dump][{}] chargingAlarm msgType={} skip", connectorId, msgType);
            }
            return;
        }

        // MeterValues DataTransfer (messageId 대소문자 처리)
        if ("MeterValues".equalsIgnoreCase(messageId)) {
            tx.meterValuesLines.add(line);
            return;
        }

        // 혹시 messageId 대신 data 내부 타입 구분 구조를 쓰는 경우 대비
        if (dataObj != null) {
            String subType = getAsStringSafe(dataObj.get("messageId"));
            if ("MeterValues".equalsIgnoreCase(subType)) {
                tx.meterValuesLines.add(line);
                return;
            }
        }

        logger.debug("[Dump][{}] 알 수 없는 DataTransfer skip. messageId={}", connectorId, messageId);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendNextTransaction(int connectorId) {
        if (dumpQueue.isEmpty()) {
            currentTx = null;
            logger.info("[Dump][{}] dump resend complete", connectorId);
            stopTask();
            GlobalVariables.setDumpSending(connectorId, false);
            return;
        }

        currentTx = dumpQueue.peek();

        if (currentTx == null) {
            logger.warn("[Dump][{}] currentTx null", connectorId);
            dumpQueue.poll();
            handler.postDelayed(() -> sendNextTransaction(connectorId), 200);
            return;
        }

        if (currentTx.authorizeLine != null) {
            sendAuthorize(connectorId);
        } else if (currentTx.startLine != null) {
            sendStartTransaction(connectorId);
        } else if (currentTx.alarmStartLine != null) {
            sendAlarmStart(connectorId);
        } else if (currentTx.meterValuesLines != null && !currentTx.meterValuesLines.isEmpty()) {
            // Start가 없지만 MeterValues가 있는 경우 MeterValues부터 시작
            sendMeterValues(connectorId);
        } else if (currentTx.stopLine != null) {
            sendStopTransaction(connectorId);
        } else {
            logger.warn("[Dump][{}] 시작 가능한 전문 없음 - tx skip", connectorId);
            dumpQueue.poll();
            currentTx = null;
            handler.postDelayed(() -> sendNextTransaction(connectorId), 200);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendAuthorize(int connectorId) {
        try {
            JsonArray arr = JsonParser.parseString(currentTx.authorizeLine).getAsJsonArray();
            JsonObject payload = arr.get(3).getAsJsonObject();  // payload 추출

            if (payload == null) {
                throw new IllegalStateException("Authorize payload is null");
            }

            String idTag = payload.get("idTag").getAsString();

            // Authorize create
            AuthorizeRequest req = new AuthorizeRequest(idTag);

            // Authorize send
            logger.info("[Dump][{}] Authorize send", connectorId);
            activity.getSocketReceiveMessage().onSend(
                    connectorId,
                    req.getActionName(),
                    req
            );

            startTimeout(connectorId); // timeout start
        } catch (Exception e) {
            logger.error("[Dump][{}] Authorize send error", connectorId, e);
            onSendFail(connectorId);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onReceiveAuthorizeConf(int connectorId) {
        cancelTimeout();    // timeout cancel

        if (currentTx == null) {
            currentTx = dumpQueue.peek();
            if (currentTx == null) {
                logger.warn("[Dump][{}] onReceiveAuthorizeConf currentTx 없음", connectorId);
                return;
            }
        }

        if (currentTx.startLine != null) {
            sendStartTransaction(connectorId);
        } else {
            logger.warn("[Dump][{}] Authorize 이후 StartTransaction 없음", connectorId);
            onSendFail(connectorId);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendStartTransaction(int connectorId) {
        try {
            JsonArray arr = JsonParser.parseString(currentTx.startLine).getAsJsonArray();
            JsonObject payload = arr.get(3).getAsJsonObject();  // payload 추출

            if (payload == null) {
                throw new IllegalStateException("StartTransaction payload is null");
            }

            Integer txConnectorId = payload.get("connectorId").getAsInt();
            String idTag = payload.get("idTag").getAsString();
            long meterStart = payload.get("meterStart").getAsLong();
            ZonedDateTime timestamp = ZonedDateTime.parse(payload.get("timestamp").getAsString());

            // StartTransactionRequest create
            StartTransactionRequest req = new StartTransactionRequest(
                    txConnectorId,
                    idTag,
                    meterStart,
                    timestamp
            );


            // StartTransaction send
            logger.info("[Dump][{}] StartTransaction send", txConnectorId);
            activity.getSocketReceiveMessage().onSend(
                    txConnectorId,
                    req.getActionName(),
                    req
            );

            startTimeout(connectorId); // timeout start
        } catch (Exception e) {
            logger.error("[Dump][{}] StartTransaction send error", connectorId, e);
            onSendFail(connectorId);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onReceiveStartTransactionConf(int connectorId, int transactionId) {
        cancelTimeout();
        GlobalVariables.setDumpTransactionId(connectorId, transactionId);

        if (currentTx == null) {
            if (dumpQueue != null && !dumpQueue.isEmpty()) currentTx = dumpQueue.peek();
            if (currentTx == null) {
                logger.warn("[Dump][{}] onReceiveStartTransactionConf currentTx 없음", connectorId);
                return;
            }
        }

        if (currentTx.alarmStartLine != null) {
            sendAlarmStart(connectorId);
        } else if (currentTx.meterValuesLines != null && !currentTx.meterValuesLines.isEmpty()) {
            sendMeterValues(connectorId);
        } else if (currentTx.stopLine != null) {
            sendStopTransaction(connectorId);
        } else if (currentTx.alarmStopLine != null) {
            sendAlarmStop(connectorId);
        } else {
            finishCurrentTransaction(connectorId);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendAlarmStart(int connectorId) {
        try {
            JsonArray arr = JsonParser.parseString(currentTx.alarmStartLine).getAsJsonArray();
            JsonObject payload = arr.get(3).getAsJsonObject();

            String vendorId = payload.get("vendorId").getAsString();
            String messageId = payload.get("messageId").getAsString();

            // data는 JsonObject가 아니라 String 형태의 JSON
            String dataString = payload.get("data").getAsString();
            JsonObject data = JsonParser.parseString(dataString).getAsJsonObject();

            int txConnectorId = data.get("connectorId").getAsInt();
            int msgType = data.get("msgType").getAsInt();
            int transactionId = GlobalVariables.getDumpTransactionId(txConnectorId);
            String idTag = data.get("idTag").getAsString();
            String phoneNum = data.get("phoneNum").getAsString();

            // 재전송용 transactionId로 교체
            data.addProperty("transactionId", transactionId);

            // 다시 payload에 문자열 형태로 세팅
            payload.addProperty("data", data.toString());

            // ChargingAlarmRequest create
            ChargingAlarmData chargingAlarmData = new ChargingAlarmData();
            chargingAlarmData.setConnectorId(txConnectorId);
            chargingAlarmData.setMsgType(msgType);
            chargingAlarmData.setTransactionId(transactionId);
            chargingAlarmData.setIdTag(idTag);
            chargingAlarmData.setPhoneNum(phoneNum);

            ChargingAlarmRequest req = new ChargingAlarmRequest();
            req.setVendorId(vendorId);
            req.setMessageId(messageId);
            Gson gson = new Gson();
            req.setData(gson.toJson(chargingAlarmData));

            // ChargingAlarm(type=1) send
            logger.info("[Dump][{}] ChargingAlarm(type=1) send", connectorId);
            activity.getSocketReceiveMessage().onSend(
                    txConnectorId,
                    req.getActionName(),
                    req
            );

            // DataTransfer conf를 별도로 기다리지 않는 구조라면 다음 단계로 바로 진행
            if (currentTx.meterValuesLines != null && !currentTx.meterValuesLines.isEmpty()) {
                logger.info("[Dump][{}] AlarmStart 이후 MeterValues 전송 예약 ({})",
                        connectorId, currentTx.meterValuesLines.size());
                handler.postDelayed(() -> sendMeterValues(connectorId), 300);
            } else if (currentTx.stopLine != null) {
                logger.info("[Dump][{}] AlarmStart 이후 StopTransaction 전송 예약", connectorId);
                handler.postDelayed(() -> sendStopTransaction(connectorId), 300);
            } else if (currentTx.alarmStopLine != null) {
                logger.info("[Dump][{}] AlarmStart 이후 AlarmStop 전송 예약", connectorId);
                handler.postDelayed(() -> sendAlarmStop(connectorId), 300);
            } else {
                handler.postDelayed(() -> finishCurrentTransaction(connectorId), 300);
            }

        } catch (Exception e) {
            logger.error("[Dump][{}] ChargingAlarm(type=1) send error", connectorId, e);
            onSendFail(connectorId);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendMeterValues(int connectorId) {
        handler.postDelayed(new Runnable() {
            int index = 0;

            @Override
            public void run() {
                if (currentTx == null) {
                    logger.warn("[Dump][{}] sendMeterValues currentTx null", connectorId);
                    return;
                }

                if (index < currentTx.meterValuesLines.size()) {
                    try {
                        String line = currentTx.meterValuesLines.get(index);
                        JsonArray arr = JsonParser.parseString(line).getAsJsonArray();

                        if (currentTx.startLine != null) {
                            replaceTransactionIdInDataTransfer(
                                    arr,
                                    GlobalVariables.getDumpTransactionId(connectorId)
                            );
                        }

                        JsonObject payload = arr.get(3).getAsJsonObject();

                        String vendorId = payload.get("vendorId").getAsString();
                        String messageId = payload.get("messageId").getAsString();

                        // data는 JsonObject가 아니라 String 형태의 JSON
                        String dataString = payload.get("data").getAsString();
                        JsonObject data = JsonParser.parseString(dataString).getAsJsonObject();
                        int txConnectorId = data.get("connectorId").getAsInt();


                        logger.info("[Dump][{}] MeterValues send ({}/{})",
                                connectorId, index + 1, currentTx.meterValuesLines.size());

                        // 다시 payload에 문자열 형태로 세팅
                        payload.addProperty("data", data.toString());

                        // MeterValues create
                        MeterValuesRequest req = new MeterValuesRequest();
                        req.setVendorId(vendorId);
                        req.setMessageId(messageId);
                        req.setData(dataString);

                        activity.getSocketReceiveMessage().onSend(
                                txConnectorId,
                                req.getActionName(),
                                req
                        );
                        index++;
                        handler.postDelayed(this, 400);

                    } catch (Exception e) {
                        logger.error("[Dump][{}] MeterValues send error", connectorId, e);
                        onSendFail(connectorId);
                    }
                } else {
                    if (currentTx.stopLine != null) {
                        sendStopTransaction(connectorId);
                    } else if (currentTx.alarmStopLine != null) {
                        sendAlarmStop(connectorId);
                    } else {
                        finishCurrentTransaction(connectorId);
                    }
                }
            }
        }, 400);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendStopTransaction(int connectorId) {
        try {
            JsonArray arr = JsonParser.parseString(currentTx.stopLine).getAsJsonArray();
            JsonObject payload = arr.get(3).getAsJsonObject();

            if (payload == null) {
                throw new IllegalStateException("StopTransaction payload is null");
            }

            String idTag = payload.get("idTag").getAsString();
            long meterStop = payload.get("meterStop").getAsLong();
            ZonedDateTime timestamp = ZonedDateTime.parse(payload.get("timestamp").getAsString());
            Reason reason = Reason.valueOf(payload.get("reason").getAsString());

            int txId;
            if (currentTx.startLine != null) {
                txId = GlobalVariables.getDumpTransactionId(connectorId);
            } else {
                txId = payload.get("transactionId").getAsInt();
            }

            // StopTransactionRequest create
            StopTransactionRequest req = new StopTransactionRequest(
                    meterStop,
                    timestamp,
                    txId,
                    reason
            );
            req.setIdTag(idTag);
            req.setTransactionData(parseTransactionData(payload.getAsJsonArray("transactionData")));

            // StopTransaction send
            logger.info("[Dump][{}] StopTransaction send", connectorId);
            activity.getSocketReceiveMessage().onSend(
                    connectorId,
                    req.getActionName(),
                    req
            );

            startTimeout(connectorId); // timeout start
        } catch (Exception e) {
            logger.error("[Dump][{}] StopTransaction send error", connectorId, e);
            onSendFail(connectorId);
        }
    }

    private MeterValue[] parseTransactionData(JsonArray txDataArray) {
        List<MeterValue> meterValues = new ArrayList<>();

        for (JsonElement meterValueEl : txDataArray) {
            JsonObject meterValueObj = meterValueEl.getAsJsonObject();

            MeterValue meterValue = new MeterValue();

            if (meterValueObj.has("timestamp") && !meterValueObj.get("timestamp").isJsonNull()) {
                meterValue.setTimestamp(
                        ZonedDateTime.parse(meterValueObj.get("timestamp").getAsString())
                );
            }

            if (meterValueObj.has("sampledValue") && meterValueObj.get("sampledValue").isJsonArray()) {
                JsonArray sampledValueArray = meterValueObj.getAsJsonArray("sampledValue");
                List<SampledValue> sampledValues = new ArrayList<>();

                for (JsonElement sampledEl : sampledValueArray) {
                    JsonObject sampledObj = sampledEl.getAsJsonObject();

                    SampledValue sv = new SampledValue();
                    sv.setValue(sampledObj.get("value").getAsString());

                    if (sampledObj.has("context") && !sampledObj.get("context").isJsonNull()) {
                        sv.setContext(sampledObj.get("context").getAsString());
                    }

                    if (sampledObj.has("format") && !sampledObj.get("format").isJsonNull()) {
                        sv.setFormat(ValueFormat.valueOf(sampledObj.get("format").getAsString()));
                    }

                    if (sampledObj.has("measurand") && !sampledObj.get("measurand").isJsonNull()) {
                        sv.setMeasurand(sampledObj.get("measurand").getAsString());
                    }

                    if (sampledObj.has("phase") && !sampledObj.get("phase").isJsonNull()) {
                        sv.setPhase(sampledObj.get("phase").getAsString());
                    }

                    if (sampledObj.has("location") && !sampledObj.get("location").isJsonNull()) {
                        sv.setLocation(Location.valueOf(sampledObj.get("location").getAsString()));
                    }

                    if (sampledObj.has("unit") && !sampledObj.get("unit").isJsonNull()) {
                        sv.setUnit(sampledObj.get("unit").getAsString());
                    }

                    sampledValues.add(sv);
                }

                meterValue.setSampledValue(sampledValues.toArray(new SampledValue[0]));
            }

            meterValues.add(meterValue);
        }

        return meterValues.toArray(new MeterValue[0]);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onReceiveStopTransactionConf(int connectorId) {
        cancelTimeout();

        if (currentTx != null && currentTx.alarmStopLine != null) {
            sendAlarmStop(connectorId);
            return;
        }

        finishCurrentTransaction(connectorId);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendAlarmStop(int connectorId) {
        try {
            JsonArray arr = JsonParser.parseString(currentTx.alarmStopLine).getAsJsonArray();
            JsonObject payload = arr.get(3).getAsJsonObject();

            String vendorId = payload.get("vendorId").getAsString();
            String messageId = payload.get("messageId").getAsString();

            // data는 JsonObject가 아니라 String 형태의 JSON
            String dataString = payload.get("data").getAsString();
            JsonObject data = JsonParser.parseString(dataString).getAsJsonObject();

            int txConnectorId = data.get("connectorId").getAsInt();
            int msgType = data.get("msgType").getAsInt();
            String idTag = data.get("idTag").getAsString();
            String phoneNum = data.get("phoneNum").getAsString();

            int txId;
            if (currentTx.startLine != null) {
                txId = GlobalVariables.getDumpTransactionId(connectorId);
            } else {
                txId = data.get("transactionId").getAsInt();
                if (txId < 0) {
                    throw new IllegalStateException("alarmStopLine transactionId 없음");
                }
            }

            // 재전송용 transactionId로 교체
            data.addProperty("transactionId", txId);

            // 다시 payload에 문자열 형태로 세팅
            payload.addProperty("data", data.toString());

            // ChargingAlarmRequest create
            ChargingAlarmData chargingAlarmData = new ChargingAlarmData();
            chargingAlarmData.setConnectorId(txConnectorId);
            chargingAlarmData.setMsgType(msgType);
            chargingAlarmData.setTransactionId(txId);
            chargingAlarmData.setIdTag(idTag);
            chargingAlarmData.setPhoneNum(phoneNum);

            ChargingAlarmRequest req = new ChargingAlarmRequest();
            req.setVendorId(vendorId);
            req.setMessageId(messageId);
            Gson gson = new Gson();
            req.setData(gson.toJson(chargingAlarmData));

            // ChargingAlarm(type=3) send
            logger.info("[Dump][{}] ChargingAlarm(type=3) send", connectorId);
            activity.getSocketReceiveMessage().onSend(
                    txConnectorId,
                    req.getActionName(),
                    req
            );

            // DataTransfer conf를 기다리지 않는 구조
            handler.postDelayed(() -> finishCurrentTransaction(connectorId), 300);
        } catch (Exception e) {
            logger.error("[Dump][{}] ChargingAlarm(type=3) send error", connectorId, e);
            onSendFail(connectorId);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void finishCurrentTransaction(int connectorId) {
        logger.info("[Dump][{}] dump transaction success", connectorId);
        dumpQueue.poll();
        currentTx = null;
        handler.postDelayed(() -> sendNextTransaction(connectorId), 500);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onSendFail(int connectorId) {
        logger.error("[Dump][{}] dump send failed -> rewrite dump file", connectorId);

        cancelTimeout();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(dumpPath, false))) {
            // 현재 tx 먼저 저장
            if (currentTx != null) {
                writeTransaction(bw, currentTx);
            }

            // queue 맨 앞(currentTx) 제거 후 나머지 저장
            if (!dumpQueue.isEmpty()) {
                dumpQueue.poll();
            }

            for (DumpTransaction tx : dumpQueue) {
                writeTransaction(bw, tx);
            }

            bw.flush();

        } catch (Exception e) {
            logger.error("[Dump][{}] dump rewrite error", connectorId, e);
        }

        currentTx = null;
        handler.postDelayed(() -> sendNextTransaction(connectorId), 500);
    }

    private void writeTransaction(BufferedWriter bw, DumpTransaction tx) throws IOException {
        if (tx == null) return;

        if (tx.authorizeLine != null) {
            bw.write(tx.authorizeLine);
            bw.newLine();
        }

        if (tx.startLine != null) {
            bw.write(tx.startLine);
            bw.newLine();
        }

        if (tx.alarmStartLine != null) {
            bw.write(tx.alarmStartLine);
            bw.newLine();
        }

        for (String meterLine : tx.meterValuesLines) {
            bw.write(meterLine);
            bw.newLine();
        }

        if (tx.stopLine != null) {
            bw.write(tx.stopLine);
            bw.newLine();
        }

        if (tx.alarmStopLine != null) {
            bw.write(tx.alarmStopLine);
            bw.newLine();
        }
    }

    private JsonObject getPayloadObject(JsonArray arr) {
        if (arr == null || arr.size() <= 3 || arr.get(3) == null || !arr.get(3).isJsonObject()) {
            return null;
        }
        return arr.get(3).getAsJsonObject();
    }

    private String getAsStringSafe(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            return element.getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private Integer getAsIntSafe(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        try {
            return element.getAsInt();
        } catch (Exception e) {
            return null;
        }
    }

    private void replaceTransactionIdInDataTransfer(JsonArray arr, int transactionId) {
        JsonObject payload = getPayloadObject(arr);
        if (payload == null) {
            throw new IllegalStateException("DataTransfer payload 없음");
        }

        JsonElement dataElement = payload.get("data");
        if (dataElement != null) {
            if (dataElement.isJsonObject()) {
                JsonObject dataObject = dataElement.getAsJsonObject();
                dataObject.addProperty("transactionId", transactionId);
                return;
            } else if (dataElement.isJsonPrimitive()) {
                JsonObject dataObject = JsonParser.parseString(dataElement.getAsString()).getAsJsonObject();
                dataObject.addProperty("transactionId", transactionId);
                payload.addProperty("data", dataObject.toString());
                return;
            }
        }

        // payload 바로 아래에 transactionId가 있는 경우까지 보조 대응
        payload.addProperty("transactionId", transactionId);
    }

    public void stopTask() {
        handler.removeCallbacksAndMessages(null);
    }

    private Runnable timeoutRunnable;

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startTimeout(int connectorId) {
        cancelTimeout();

        timeoutRunnable = () -> {
            logger.warn("Dump transaction timeout");
            onSendFail(connectorId);
        };

        handler.postDelayed(timeoutRunnable, 10000);
    }

    private void cancelTimeout() {
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
        }
    }
}
