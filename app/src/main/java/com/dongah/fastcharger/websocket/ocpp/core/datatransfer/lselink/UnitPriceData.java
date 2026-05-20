package com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink;

import com.dongah.fastcharger.websocket.ocpp.common.model.Validatable;

import java.util.Objects;

public class UnitPriceData implements Validatable {

    private String chargeBoxSerialNumber;   // 충전소ID
    private String chargePointSerialNumber; // 충전기ID
    private int connectorId;                // 충전건ID

    public UnitPriceData() {
    }

    public String getChargeBoxSerialNumber() {
        return chargeBoxSerialNumber;
    }

    public void setChargeBoxSerialNumber(String chargeBoxSerialNumber) {
        this.chargeBoxSerialNumber = chargeBoxSerialNumber;
    }

    public String getChargePointSerialNumber() {
        return chargePointSerialNumber;
    }

    public void setChargePointSerialNumber(String chargePointSerialNumber) {
        this.chargePointSerialNumber = chargePointSerialNumber;
    }

    public int getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(int connectorId) {
        this.connectorId = connectorId;
    }

    @Override
    public boolean validate() { return true; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnitPriceData that = (UnitPriceData) o;
        return Objects.equals(chargeBoxSerialNumber, that.chargeBoxSerialNumber) &&
                Objects.equals(chargePointSerialNumber, that.chargePointSerialNumber) &&
                connectorId == that.connectorId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(chargeBoxSerialNumber, chargePointSerialNumber, connectorId);
    }
}
