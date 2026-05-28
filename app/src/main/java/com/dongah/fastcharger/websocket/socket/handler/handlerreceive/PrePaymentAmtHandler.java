package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.websocket.ocpp.core.DataTransferStatus;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.PrePaymentAmtConfirm;
import com.dongah.fastcharger.websocket.socket.OcppHandler;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrePaymentAmtHandler implements OcppHandler {
    private static final Logger logger = LoggerFactory.getLogger(PrePaymentAmtHandler.class);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        try {
            JSONObject dataJson = payload.getJSONObject("data");

            MainActivity activity = (MainActivity) MainActivity.mContext;

            PrePaymentAmtConfirm prePaymentAmtConfirm = new PrePaymentAmtConfirm();
            prePaymentAmtConfirm.setStatus(DataTransferStatus.Accepted);

            // response
            activity.getSocketReceiveMessage().onResultSend(
                    connectorId,
                    prePaymentAmtConfirm.getActionName(),
                    messageId,
                    prePaymentAmtConfirm
            );

            prePayment(connectorId, dataJson);
        } catch (Exception e) {
            logger.error("PrePaymentAmtHandelr error : {}", e.getMessage(), e);
        }
    }

    private void prePayment(int connectorId, JSONObject dataJson) {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(connectorId-1);

            String idTag = dataJson.getString("idTag");
            double rechgKw = dataJson.getDouble("rechgKw");
            int rechgAmt = dataJson.getInt("rechgAmt");

            chargingCurrentData.setIdTag(idTag);

            logger.info("idTag: {}, rechgKw: {}, rechgAmt: {}", idTag, rechgKw, rechgAmt);
        } catch (Exception e) {
            logger.error("prePayment error : {}", e.getMessage(), e);
        }
    }
}
