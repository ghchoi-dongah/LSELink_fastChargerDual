package com.dongah.fastcharger.websocket.ocpp.core.datatransfer.vas;

import com.dongah.fastcharger.websocket.ocpp.common.model.Validatable;

public class BatteryInfoDataSend implements Validatable {

    private String timeStamp;
    private String battery;


    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getBattery() {
        return battery;
    }

    public void setBattery(String battery) {
        this.battery = battery;
    }

    @Override
    public boolean validate() {
        return true;
    }
}
