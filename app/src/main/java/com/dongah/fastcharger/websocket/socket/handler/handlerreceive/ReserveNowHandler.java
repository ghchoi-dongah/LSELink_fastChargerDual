package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.controlboard.RxData;
import com.dongah.fastcharger.utils.FileManagement;
import com.dongah.fastcharger.utils.SupportFunction;
import com.dongah.fastcharger.websocket.ocpp.core.ChargePointStatus;
import com.dongah.fastcharger.websocket.ocpp.reservation.ReservationStatus;
import com.dongah.fastcharger.websocket.ocpp.reservation.ReserveNowConfirmation;
import com.dongah.fastcharger.websocket.socket.OcppHandler;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.StatusNotificationReq;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

public class ReserveNowHandler implements OcppHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReserveNowHandler.class);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        try {
            int resConnectorId = payload.has("connectorId") ? payload.getInt("connectorId") : 0;
            String resExpiryDate = payload.has("expiryDate") ? payload.getString("expiryDate") : "";
            String resIdTag = payload.has("idTag") ? payload.getString("idTag") : "";
            String resParentIdTag = payload.has("parentIdTag") ? payload.getString("parentIdTag") : "";
            String resReservationId = payload.has("reservationId") ? payload.getString("reservationId") : "";

            MainActivity activity = (MainActivity) MainActivity.mContext;
            ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(connectorId-1);
            boolean faultedCase = false, occupiedCase = false, unavailableCase = false;

            if (GlobalVariables.isReserveConnectorZeroSupported() &&  resConnectorId == 0) {
                RxData rxData0 = activity.getControlBoard().getRxData(0);
                RxData rxData1 = activity.getControlBoard().getRxData(1);
                faultedCase = rxData0.isCsFault() || rxData1.isCsFault();
                occupiedCase = activity.getChargingCurrentData(0).getChargePointStatus() == ChargePointStatus.Available ||
                        activity.getChargingCurrentData(1).getChargePointStatus() == ChargePointStatus.Available;
                unavailableCase = GlobalVariables.ChargerOperation[1] || GlobalVariables.ChargerOperation[2];
            } else if (resConnectorId > 0) {
                int resChannel = resConnectorId - 1;
                chargingCurrentData = activity.getChargingCurrentData(resChannel);
                RxData rxData = activity.getControlBoard().getRxData(resChannel);
                faultedCase = rxData.isCsFault() || activity.getClassUiProcess(resChannel).getUiSeq() != UiSeq.INIT;
                occupiedCase = chargingCurrentData.getReservedStatus() == ChargePointStatus.Available
                        && chargingCurrentData.getChargePointStatus() == ChargePointStatus.Available;
                unavailableCase = GlobalVariables.ChargerOperation[resConnectorId];
            }

            //configuration key SupportedFeatureProfiles check
            SupportFunction supportFunction = new SupportFunction();
            boolean reserveSupported = supportFunction.onSupportedFeatureProfiles("Reservation") ;

            ReservationStatus reservationStatus;
            reservationStatus = (!reserveSupported || resConnectorId == 0 ? ReservationStatus.Rejected : faultedCase ? ReservationStatus.Faulted :
                    !unavailableCase ? ReservationStatus.Unavailable : !occupiedCase ? ReservationStatus.Occupied :
                            ReservationStatus.Accepted);

            ReserveNowConfirmation reserveNowConfirmation = new ReserveNowConfirmation(reservationStatus);
            activity.getSocketReceiveMessage().onResultSend(
                    resConnectorId,
                    reserveNowConfirmation.getActionName(),
                    messageId,
                    reserveNowConfirmation
            );

            if (Objects.equals(reservationStatus, ReservationStatus.Accepted)) {
                chargingCurrentData.setResConnectorId(resConnectorId);
                chargingCurrentData.setResExpiryDate(resExpiryDate);
                chargingCurrentData.setResIdTag(resIdTag);
                chargingCurrentData.setResParentIdTag(resParentIdTag);
                chargingCurrentData.setResReservationId(resReservationId);
                chargingCurrentData.setChargePointStatus(ChargePointStatus.Reserved);
                chargingCurrentData.setReservedStatus(ChargePointStatus.Reserved);

                // StatusNotification(Reserved)
                StatusNotificationReq statusNotificationReq = new StatusNotificationReq(connectorId);
                statusNotificationReq.sendStatusNotification(resConnectorId, chargingCurrentData.getChargePointStatus());
            }
        } catch (Exception e) {
            logger.error("ReserveNowHandler error : {}", e.getMessage(), e);
        }
    }
}
