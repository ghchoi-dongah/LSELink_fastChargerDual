package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.os.Build.VERSION_CODES;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.FragmentChange;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.utils.LogDataSave;
import com.dongah.fastcharger.websocket.ocpp.core.ChargePointStatus;
import com.dongah.fastcharger.websocket.ocpp.core.StartTransactionRequest;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;
import com.dongah.fastcharger.websocket.socket.SocketState;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

public class StartTransactionReq {
    private static final Logger logger = LoggerFactory.getLogger(StartTransactionReq.class);

    private final int connectorId ;

    final ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();

    public int getConnectorId() {
        return connectorId;
    }

    public StartTransactionReq(int connectorId) {
        this.connectorId = connectorId;
    }


    @RequiresApi(api = VERSION_CODES.O)
    public void sendStartTransactionReq() {

        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(getConnectorId()-1);

            double meterStart = chargingCurrentData.getPowerMeterStart();
            String idTag = chargingCurrentData.getIdTag();

            chargingCurrentData.setChargingStartTime(zonedDateTimeConvert.getStringCurrentTimeZone());

            ZonedDateTime timestamp = zonedDateTimeConvert.doZonedDateTimeToDatetime(chargingCurrentData.getChargingStartTime());
            StartTransactionRequest startTransactionRequest = new StartTransactionRequest(getConnectorId(), idTag, (long) (meterStart), timestamp);

            if (!TextUtils.isEmpty(chargingCurrentData.getResParentIdTag()) || !TextUtils.isEmpty(chargingCurrentData.getResIdTag())) {
                if (Objects.equals(chargingCurrentData.getIdTag(), chargingCurrentData.getResIdTag()) ||
                        Objects.equals(chargingCurrentData.getParentIdTag(), chargingCurrentData.getResParentIdTag())) {
                    startTransactionRequest.setReservationId(Integer.parseInt(chargingCurrentData.getResReservationId()));
                }
            }


            SocketState socketState = activity.getSocketReceiveMessage().getSocket().getState();
            if (socketState.equals(SocketState.OPEN)) {
                //send
                activity.getSocketReceiveMessage().onSend(
                        getConnectorId(),
                        startTransactionRequest.getActionName(),
                        startTransactionRequest);
            } else {
                //통신이 안되면 저장
                String uuid = UUID.randomUUID().toString();
                saveFullStartTransaction(getConnectorId(), uuid, startTransactionRequest);

                // DataTransfer ChargingAlarm
                ChargingAlarmReq chargingAlarmReq = new ChargingAlarmReq(connectorId);
                chargingAlarmReq.sendChargingAlarmReq(1);

                //화면 전환
                chargingCurrentData.setChargePointStatus(ChargePointStatus.Charging);
                activity.getClassUiProcess(getConnectorId()-1).setUiSeq(UiSeq.CHARGING);
                FragmentChange fragmentChange = new FragmentChange();
                fragmentChange.onFragmentChange(getConnectorId()-1, UiSeq.CHARGING, "CHARGING", null);
            }

        } catch (Exception e) {
            logger.error("sendStartTransactionReq error : {}", e.getMessage());
        }
    }


    private void saveFullStartTransaction(
            int connectorId,
            String uniqueId,
            StartTransactionRequest req) {
        try {
            JSONArray frame = new JSONArray();

            frame.put(2); // CALL
            frame.put(uniqueId);
            frame.put(req.getActionName());

            JSONObject payload = new JSONObject();
            payload.put("connectorId", req.getConnectorId());
            payload.put("idTag", req.getIdTag());
            payload.put("meterStart", req.getMeterStart());
            payload.put("timestamp", req.getTimestamp().toString());

            if (req.getReservationId() != null) {
                payload.put("reservationId", req.getReservationId());
            }

            frame.put(payload);

            LogDataSave logDataSave = new LogDataSave();
            logDataSave.makeDump(connectorId, frame.toString());
        } catch (Exception e) {
            logger.error("saveFullStartTransaction error : {}", e.getMessage());
        }
    }
}
