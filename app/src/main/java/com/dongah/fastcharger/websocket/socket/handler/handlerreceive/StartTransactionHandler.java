package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.FragmentChange;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.controlboard.TxData;
import com.dongah.fastcharger.websocket.ocpp.core.AuthorizationStatus;
import com.dongah.fastcharger.websocket.ocpp.core.ChargePointStatus;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;
import com.dongah.fastcharger.websocket.socket.OcppHandler;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.ChargingAlarmReq;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.FullRechgSocReq;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.StatusNotificationReq;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.StopTransactionReq;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.UserSetSocReq;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class StartTransactionHandler implements OcppHandler  {
    private static final Logger logger = LoggerFactory.getLogger(StartTransactionHandler.class);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        MainActivity activity = ((MainActivity) MainActivity.mContext);
        ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(connectorId-1);
        //서버에서 transactionId 받음 ==> stopTransaction 계속하여 사용.
        chargingCurrentData.setTransactionId(payload.getInt("transactionId"));

        JSONObject idTagInfo = payload.getJSONObject("idTagInfo");
        AuthorizationStatus status = AuthorizationStatus.valueOf(idTagInfo.getString("status"));
        String parentIdTag = idTagInfo.has("parentIdTag") ? idTagInfo.getString("parentIdTag") : "";

        // dump data
        if (GlobalVariables.isDumpSending(connectorId)) {
            logger.info("Dump StartTransaction Conf 수신 : {}", payload.getInt("transactionId"));
            GlobalVariables.setDumpTransactionId(connectorId, payload.getInt("transactionId"));
            activity.getSocketReceiveMessage().getSocket()
                    .getDumpDataSend(connectorId).onReceiveStartTransactionConf(connectorId, payload.getInt("transactionId"));
            return;
        }

        //accept continue
        if (Objects.equals(status, AuthorizationStatus.Accepted)) {
            chargingCurrentData.setChargePointStatus(ChargePointStatus.Charging);

            // DataTransfer ChargingAlarm
            ChargingAlarmReq chargingAlarmReq = new ChargingAlarmReq(connectorId);
            chargingAlarmReq.sendChargingAlarmReq(1);

            // DataTransfer fullrechgsoc
            FullRechgSocReq fullRechgSocReq = new FullRechgSocReq(connectorId);
            fullRechgSocReq.sendFullRechSoc();

            // StatusNotification
            StatusNotificationReq statusNotificationReq = new StatusNotificationReq(connectorId);
            statusNotificationReq.sendStatusNotification(connectorId, ChargePointStatus.Charging);

            // DataTransfer userSetSoc
            UserSetSocReq userSetSocReq = new UserSetSocReq(connectorId);
            userSetSocReq.sendUserSetSoc();

            activity.getClassUiProcess(connectorId-1).setUiSeq(UiSeq.CHARGING);
            FragmentChange fragmentChange = new FragmentChange();
            fragmentChange.onFragmentChange(connectorId-1, UiSeq.CHARGING, "CHARGING", null);
        } else {
            // stop
            TxData txData = activity.getControlBoard().getTxData(connectorId-1);
            txData.setStop(true);
            txData.setStart(false);
            txData.setUiSequence((short) 3);

            // DataTransfer MeterValues
            activity.getClassUiProcess(connectorId-1).onMeterValueStop();

            // StopTransaction
            ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();
            chargingCurrentData.setPowerMeterStop(chargingCurrentData.getPowerMeterStart());
            chargingCurrentData.setChargingEndTime(zonedDateTimeConvert.getStringCurrentTimeZone());
            StopTransactionReq stopTransactionReq = new StopTransactionReq(connectorId);
            stopTransactionReq.sendStopTransactionReq();

            // home
            activity.getClassUiProcess(connectorId-1).onHome();
        }
    }
}
