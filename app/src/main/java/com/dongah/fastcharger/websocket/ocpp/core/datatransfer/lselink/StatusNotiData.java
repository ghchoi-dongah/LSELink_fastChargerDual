package com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink;

import com.dongah.fastcharger.websocket.ocpp.common.model.Validatable;

import java.util.Objects;

public class StatusNotiData implements Validatable {
    private int connectorId;
    private ModeStatus status;

    public StatusNotiData() {}

    public int getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(int connectorId) {
        this.connectorId = connectorId;
    }

    public ModeStatus getStatus() {
        return status;
    }

    public void setStatus(ModeStatus status) {
        this.status = status;
    }

    @Override
    public boolean validate() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StatusNotiData that = (StatusNotiData) o;
        return connectorId == that.connectorId && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectorId, status);
    }
}
