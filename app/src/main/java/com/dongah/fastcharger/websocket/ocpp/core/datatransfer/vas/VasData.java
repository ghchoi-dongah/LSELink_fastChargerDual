package com.dongah.fastcharger.websocket.ocpp.core.datatransfer.vas;

import com.dongah.fastcharger.websocket.ocpp.common.model.Validatable;

import java.util.List;
import java.util.Map;

public class VasData implements Validatable {


    private String infoCnt;
    private List<Map<String, String>> batteryData;


    public String getInfoCnt() {
        return infoCnt;
    }

    public void setInfoCnt(String infoCnt) {
        this.infoCnt = infoCnt;
    }

    public List<Map<String, String>> getBatteryData() {
        return batteryData;
    }

    public void setBatteryData(List<Map<String, String>> batteryData) {
        this.batteryData = batteryData;
    }

    @Override
    public boolean validate() {
        return true;
    }
}
