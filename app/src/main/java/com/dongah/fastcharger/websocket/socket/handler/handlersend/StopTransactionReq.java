package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.utils.LogDataSave;
import com.dongah.fastcharger.websocket.ocpp.common.OccurenceConstraintException;
import com.dongah.fastcharger.websocket.ocpp.core.Location;
import com.dongah.fastcharger.websocket.ocpp.core.MeterValue;
import com.dongah.fastcharger.websocket.ocpp.core.SampledValue;
import com.dongah.fastcharger.websocket.ocpp.core.StopTransactionRequest;
import com.dongah.fastcharger.websocket.ocpp.core.ValueFormat;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;
import com.dongah.fastcharger.websocket.socket.SocketState;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StopTransactionReq {
    private static final Logger logger = LoggerFactory.getLogger(StopTransactionReq.class);

    private final int connectorId ;
    public int getConnectorId() {
        return connectorId;
    }

    final ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();

    public StopTransactionReq(int connectorId) {
        this.connectorId = connectorId;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendStopTransactionReq() {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(getConnectorId()-1);
            ZonedDateTime timestamp = zonedDateTimeConvert.doZonedDateTimeToDatetime(chargingCurrentData.getChargingEndTime());
            
            activity.getClassUiProcess(getConnectorId()-1).onMeterValueStop();

            StopTransactionRequest stopTransactionRequest = new StopTransactionRequest(
                    chargingCurrentData.getPowerMeterStop(),
                    timestamp,
                    chargingCurrentData.getTransactionId(),
                    chargingCurrentData.getStopReason()
            );
            stopTransactionRequest.setIdTag(chargingCurrentData.getIdTag());


            // 충전 사용량
            SampledValue energy = new SampledValue();
            energy.setValue(String.valueOf(chargingCurrentData.getPowerMeterUse() * 0.01));
            energy.setContext("Transaction.End");
            energy.setFormat(ValueFormat.Raw);
            energy.setMeasurand("Current.Export");
            energy.setPhase("L1");
            energy.setLocation(Location.Outlet);
            energy.setUnit("kWh");

            //SoC
            SampledValue soc = new SampledValue();
            soc.setValue(String.valueOf(chargingCurrentData.getSoc()));
            soc.setContext("Transaction.End");
            soc.setFormat(ValueFormat.Raw);
            soc.setMeasurand("SoC");
            soc.setPhase("L2");
            soc.setLocation(Location.EV);
            soc.setUnit("Percent");

            List<SampledValue> list = new ArrayList<>();
            list.add(energy);
            list.add(soc);
            SampledValue[] sampledArray = list.toArray(new SampledValue[0]);

            ZonedDateTime now = zonedDateTimeConvert.doGetCurrentTime();

            MeterValue meterValue = new MeterValue(timestamp, sampledArray);
            stopTransactionRequest.setTransactionData(new MeterValue[] {meterValue});


            SocketState socketState = activity.getSocketReceiveMessage().getSocket().getState();
            if (socketState.equals(SocketState.OPEN)) {
                //send
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        activity.getSocketReceiveMessage().onSend(
                                getConnectorId(),
                                stopTransactionRequest.getActionName(),
                                stopTransactionRequest);
                    } catch (OccurenceConstraintException e) {
                        logger.error("StopTransactionRequest send error ", e);
                    }
                }, 3000);
            } else {
                String uuid = UUID.randomUUID().toString();
                saveFullStartTransaction(getConnectorId(), uuid, stopTransactionRequest);

                // DataTransfer ChargingAlarm
                ChargingAlarmReq chargingAlarmReq = new ChargingAlarmReq(connectorId);
                chargingAlarmReq.sendChargingAlarmReq(3);
            }
        } catch (Exception e) {
            logger.error("sendStopTransactionReq error : {}", e.getMessage());
        }
    }

    private void saveFullStartTransaction(
            int connectorId,
            String uniqueId,
            StopTransactionRequest req) {
        try {
            JSONArray frame = new JSONArray();

            frame.put(2); // CALL
            frame.put(uniqueId);
            frame.put(req.getActionName());

            JSONObject payload = new JSONObject();
            payload.put("idTag", req.getIdTag());
            payload.put("meterStop", req.getMeterStop());
            payload.put("timestamp", req.getTimestamp().toString());
            payload.put("transactionId", req.getTransactionId());
            payload.put("reason", req.getReason());
            payload.put("transactionData", meterValuesToJsonArray(req.getTransactionData()));

            frame.put(payload);

            LogDataSave logDataSave = new LogDataSave();
            logDataSave.makeDump(connectorId, frame.toString());

        } catch (Exception e) {
            logger.error(" saveFullStartTransaction error : {}", e.getMessage());
        }
    }

    /**
     * MeterValue[] → JSONArray 직렬화
     * [
     *   {
     *     "timestamp": "...",
     *     "sampledValue": [
     *       { "value": "...", "context": "...", ... }
     *     ]
     *   }
     * ]
     */
    private JSONArray meterValuesToJsonArray(MeterValue[] meterValues) throws Exception {
        JSONArray result = new JSONArray();
        if (meterValues == null) return result;

        for (MeterValue mv : meterValues) {
            JSONObject mvObj = new JSONObject();
            mvObj.put("timestamp", mv.getTimestamp().toString());

            JSONArray sampledArr = new JSONArray();
            if (mv.getSampledValue() != null) {
                for (SampledValue sv : mv.getSampledValue()) {
                    JSONObject svObj = new JSONObject();
                    if (sv.getValue()     != null) svObj.put("value",     sv.getValue());
                    if (sv.getContext()   != null) svObj.put("context",   sv.getContext());
                    if (sv.getFormat()    != null) svObj.put("format",    sv.getFormat().name());
                    if (sv.getMeasurand() != null) svObj.put("measurand", sv.getMeasurand());
                    if (sv.getPhase()     != null) svObj.put("phase",     sv.getPhase());
                    if (sv.getLocation()  != null) svObj.put("location",  sv.getLocation().name());
                    if (sv.getUnit()      != null) svObj.put("unit",      sv.getUnit());
                    sampledArr.put(svObj);
                }
            }
            mvObj.put("sampledValue", sampledArr);
            result.put(mvObj);
        }
        return result;
    }

}
