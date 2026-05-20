package com.dongah.fastcharger.websocket.ocpp.core.datatransfer.vas;

import com.dongah.fastcharger.websocket.ocpp.common.model.Validatable;

import java.util.List;
import java.util.Map;

public class BatteryData implements Validatable {

    private String vin;
    private String soc;
    private String soh;
    private String bpa;
    private String bpv;
    private List<Map<String, String>> bsv;
    private List<Map<String, String>> bmt;

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getSoc() {
        return soc;
    }

    public void setSoc(String soc) {
        this.soc = soc;
    }

    public String getSoh() {
        return soh;
    }

    public void setSoh(String soh) {
        this.soh = soh;
    }

    public String getBpa() {
        return bpa;
    }

    public void setBpa(String bpa) {
        this.bpa = bpa;
    }

    public String getBpv() {
        return bpv;
    }

    public void setBpv(String bpv) {
        this.bpv = bpv;
    }

    public List<Map<String, String>> getBsv() {
        return bsv;
    }

    public void setBsv(List<Map<String, String>> bsv) {
        this.bsv = bsv;
    }

    public List<Map<String, String>> getBmt() {
        return bmt;
    }

    public void setBmt(List<Map<String, String>> bmt) {
        this.bmt = bmt;
    }

    @Override
    public boolean validate() {
        return true;
    }
}
