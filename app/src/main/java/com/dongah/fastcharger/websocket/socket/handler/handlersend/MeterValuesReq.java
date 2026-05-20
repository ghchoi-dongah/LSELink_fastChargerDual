package com.dongah.fastcharger.websocket.socket.handler.handlersend;


import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.utils.LogDataSave;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.MeterValuesData;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.MeterValuesRequest;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;
import com.dongah.fastcharger.websocket.socket.SocketState;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;


public class MeterValuesReq {
    private static final Logger logger = LoggerFactory.getLogger(MeterValuesReq.class);


    private final int connectorId ;

    /**  meter values handler */
    private final Handler meterHandler = new Handler(Looper.getMainLooper());
    private Runnable meterRunnable;
    private boolean isMeterRunning = false;
    private int lastIntervalSec = -1;
    private long prevPowerMeter = -1;


    public int getConnectorId() {
        return connectorId;
    }

    public MeterValuesReq(int connectorId) {
        this.connectorId = connectorId;
    }

    /** MeterValues 시작 */
    public void startMeterValues() {

        int intervalSec = GlobalVariables.getMeterValueSampleInterval();
        /// 이미 실행 중이고 interval 같으면 무시
        if (isMeterRunning && intervalSec == lastIntervalSec) {
            return;
        }

        stopMeterValues(); // 기존 중지
        lastIntervalSec = intervalSec;
        isMeterRunning = true;

        meterRunnable = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                if (!isMeterRunning) return;
                try {
                    sendMeterValues(connectorId);
                } catch (Exception e) {
                    logger.error("meterRunnable error : {}", e.getMessage());
                }
                meterHandler.postDelayed(this, intervalSec * 1000L);
            }
        };
        meterHandler.post(meterRunnable); // 즉시 1회 실행
    }

    public void stopMeterValues() {
        isMeterRunning = false;
        if (meterRunnable != null) {
            meterHandler.removeCallbacks(meterRunnable);
            meterRunnable = null;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void sendMeterValues(int connectorId) throws Exception {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        if (activity == null) return;
        // 충전 상태가 아니면 중지
        ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(connectorId-1);
//        if (!Objects.equals(chargingCurrentData.getChargePointStatus(), ChargePointStatus.Charging) ||
//                !GlobalVariables.isTriggerSet()) {
//            stopMeterValues();
//            return;
//        }

        ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();
        ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();

        long currentPowerMeter = chargingCurrentData.getPowerMeter();
        long diffPowerMeter = 0;
        if (prevPowerMeter >= 0) {
            diffPowerMeter = currentPowerMeter - prevPowerMeter;
        }
        // 다음 비교를 위해 현재값 저장
        prevPowerMeter = currentPowerMeter;

        //1. meterValuesData 생성
        MeterValuesData meterValuesData = new MeterValuesData();
        meterValuesData.chargeBoxSerialNumber = chargerConfiguration.getChargeBoxSerialNumber();
        meterValuesData.chargePointSerialNumber = chargerConfiguration.getChargerId();
        meterValuesData.connectorId = getConnectorId();
        meterValuesData.transactionId = chargingCurrentData.getTransactionId();
        meterValuesData.idTag = chargingCurrentData.getIdTag();
        meterValuesData.timestamp = zonedDateTimeConvert.doGetKstDatetimeAsString();
        meterValuesData.power = (float) ((chargingCurrentData.getOutPutVoltage() * 10) * (chargingCurrentData.getOutPutCurrent() * 0.001));
        meterValuesData.eps = (int) (chargingCurrentData.getOutPutVoltage() * 10);
        meterValuesData.ecu = (int) (chargingCurrentData.getOutPutCurrent() * 0.001) ;
        meterValuesData.accWh = (float) (chargingCurrentData.getPowerMeter() * 10);
        meterValuesData.accTickWh = (float) (diffPowerMeter *10);
        meterValuesData.accTickTime = GlobalVariables.getMeterValueSampleInterval();
        meterValuesData.rechgHr = (int) chargingCurrentData.getChargingTime();
        meterValuesData.remnHr = chargingCurrentData.getRemaintime() / 60;
        meterValuesData.btrRm = chargingCurrentData.getSoc();
        meterValuesData.slprcUpc = (float) chargingCurrentData.getPowerUnitPrice();
        meterValuesData.crtrUpc = 0.0f;

        // 2. JSON 변환
        Gson gson = new Gson();
        String jsonData = gson.toJson(meterValuesData);

        // 3. Request 생성
        MeterValuesRequest meterValuesRequest = new MeterValuesRequest();
        meterValuesRequest.setVendorId(chargerConfiguration.getChargePointVendor());
        meterValuesRequest.setMessageId("MeterValues");
        meterValuesRequest.setData(jsonData);

        // 4. 전송
        SocketState socketState = activity.getSocketReceiveMessage().getSocket().getState();
        if (socketState.equals(SocketState.OPEN)) {
            activity.getSocketReceiveMessage().onSend(
                    connectorId,
                    meterValuesRequest.getActionName(), // DataTransfer
                    meterValuesRequest);
        } else {
            // 통신이 안되면 저장
            String uuid = UUID.randomUUID().toString();
            saveFullMeterValues(getConnectorId(), uuid, meterValuesRequest);
        }
    }

    private void saveFullMeterValues(
            int connectorId,
            String uniqueId,
            MeterValuesRequest req) {
        try {
            JSONArray frame = new JSONArray();

            frame.put(2);
            frame.put(uniqueId);
            frame.put(req.getActionName());

            JSONObject payload = new JSONObject();
            payload.put("vendorId", req.getVendorId());
            payload.put("messageId", req.getMessageId());
            payload.put("data", req.getData());

            frame.put(payload);

            LogDataSave logDataSave = new LogDataSave();
            logDataSave.makeDump(connectorId, frame.toString());
        } catch (Exception e) {
            logger.error("saveFullMeterValues error : {}", e.getMessage());
        }
    }
}
