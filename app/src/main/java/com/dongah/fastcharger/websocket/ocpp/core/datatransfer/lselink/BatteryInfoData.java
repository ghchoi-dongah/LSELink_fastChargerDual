package com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink;

import com.dongah.fastcharger.websocket.ocpp.common.model.Validatable;

import java.util.Objects;

public class BatteryInfoData implements Validatable {

    private String infoCnt;         // 배열 개수
    private String connectorId;     // Connector ID
    private String tsdt;            // 충전 시작 일시
    private String keyId;           // Key ID
    private BatteryData batteryData;     // 암호화 배터리 정보

    public BatteryInfoData() {}

    public String getInfoCnt() {
        return infoCnt;
    }

    public void setInfoCnt(String infoCnt) {
        this.infoCnt = infoCnt;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(String connectorId) {
        this.connectorId = connectorId;
    }

    public String getTsdt() {
        return tsdt;
    }

    public void setTsdt(String tsdt) {
        this.tsdt = tsdt;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public BatteryData getBatteryData() {
        return batteryData;
    }

    public void setBatteryData(BatteryData batteryData) {
        this.batteryData = batteryData;
    }

    @Override
    public boolean validate() { return true; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatteryInfoData that = (BatteryInfoData) o;
        return Objects.equals(infoCnt, that.infoCnt) &&
                Objects.equals(connectorId, that.connectorId) &&
                Objects.equals(tsdt, that.tsdt) &&
                Objects.equals(keyId, that.keyId) &&
                Objects.equals(batteryData, that.batteryData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(infoCnt, connectorId, tsdt, keyId, batteryData);
    }
}
