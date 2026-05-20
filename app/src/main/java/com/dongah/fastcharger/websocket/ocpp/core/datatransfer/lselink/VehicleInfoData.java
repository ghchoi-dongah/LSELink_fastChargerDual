package com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink;

import com.dongah.fastcharger.websocket.ocpp.common.model.Validatable;

import java.util.Objects;

public class VehicleInfoData implements Validatable {

    private int connectorId;    // Connector ID
    private String idTag;       // ID Tag
    private String evccId;      // 차량 MAC 주소

    public VehicleInfoData() {}

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

    public String getEvccId() {
        return evccId;
    }

    public void setEvccId(String evccId) {
        this.evccId = evccId;
    }

    @Override
    public boolean validate() { return true; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VehicleInfoData that = (VehicleInfoData) o;
        return connectorId == that.connectorId &&
                Objects.equals(idTag, that.idTag) &&
                Objects.equals(evccId, that.evccId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectorId, idTag, evccId);
    }
}
