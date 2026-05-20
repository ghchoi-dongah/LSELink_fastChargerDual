package com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink;

import com.dongah.fastcharger.websocket.ocpp.common.model.Validatable;

import java.util.Objects;

public class FullRechgSocData implements Validatable {


    private String chargeBoxSerialNumber;
    private String chargePointSerialNumber;
    private int connectorId;
    private String idTag;
    private String timestamp;

    public FullRechgSocData() {}

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

    public String getIdTag() {
        return idTag;
    }

    public void setIdTag(String idTag) {
        this.idTag = idTag;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean validate() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FullRechgSocData that = (FullRechgSocData) o;
        return connectorId == that.connectorId &&
                chargeBoxSerialNumber.equals(that.chargeBoxSerialNumber) &&
                Objects.equals(chargePointSerialNumber, that.chargePointSerialNumber) &&
                Objects.equals(idTag, that.idTag) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectorId, chargeBoxSerialNumber, chargePointSerialNumber, idTag);
    }
}
