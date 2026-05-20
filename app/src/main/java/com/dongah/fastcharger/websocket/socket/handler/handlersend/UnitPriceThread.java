package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.websocket.ocpp.common.OccurenceConstraintException;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.UnitPriceData;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.UnitPriceRequest;
import com.google.gson.Gson;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitPriceThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(UnitPriceThread.class);

    private volatile boolean stopped = false;
    private final int delayTime;
    private int count = 0;


    @RequiresApi(api = Build.VERSION_CODES.O)
    public UnitPriceThread(int delayTime) {
        this.delayTime = delayTime;
        new Handler(Looper.getMainLooper()).postDelayed(()  -> {
            try {
                processUnitPrice();
            } catch (OccurenceConstraintException e) {
                throw new RuntimeException(e);
            }
        }, 3600);
    }

    public void stopThread() {
        stopped = true;
        interrupt(); // sleep 깨우기
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        logger.info("UnitpriceThread started");
        while (!stopped && !isInterrupted()) {

            try {
                Thread.sleep(1000);
                count++;
                if (count >= delayTime) {
                    count = 0;
                    processUnitPrice();
                }
            } catch (InterruptedException e) {
                logger.info("BootNotificationThread interrupted");
                break;
            } catch (Exception e) {
                logger.error("BootNotificationThread error", e);
            }
        }
        logger.info("BootNotificationThread terminated");
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void processUnitPrice() throws OccurenceConstraintException {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        if (activity == null) return;
        ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();

        UnitPriceData unitPriceData = new UnitPriceData();
        unitPriceData.setChargeBoxSerialNumber(chargerConfiguration.getChargeBoxSerialNumber());
        unitPriceData.setChargePointSerialNumber(chargerConfiguration.getChargePointSerialNumber());
        unitPriceData.setConnectorId(0);

        UnitPriceRequest unitPriceRequest = new UnitPriceRequest();
        unitPriceRequest.setVendorId(chargerConfiguration.getChargePointVendor());
        unitPriceRequest.setMessageId("unitprice.req");
        Gson gson = new Gson();
        unitPriceRequest.setData(gson.toJson(unitPriceData));

        activity.getSocketReceiveMessage().onSend(
                100,
                unitPriceRequest.getActionName(),
                unitPriceRequest);
    }
}
