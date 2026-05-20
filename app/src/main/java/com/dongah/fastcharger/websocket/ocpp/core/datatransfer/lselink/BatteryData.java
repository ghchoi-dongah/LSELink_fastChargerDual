package com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink;

import com.dongah.fastcharger.websocket.ocpp.common.model.Validatable;

import java.util.Objects;

public class BatteryData implements Validatable {

    private String timestamp;
    private String battery;

    public BatteryData() {}

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getBattery() {
        return battery;
    }

    public void setBattery(String battery) {
        this.battery = battery;
    }

    @Override
    public boolean validate() { return true; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatteryData that = (BatteryData) o;
        return Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(battery, that.battery);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, battery);
    }
}
