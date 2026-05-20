package com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink;

import com.dongah.fastcharger.websocket.ocpp.common.model.Validatable;

import java.util.Objects;

public class AuthorizeData implements Validatable {

    private String uuid;        // Authorize의 유니크 아이디
                                // Authorize 이력 추적을 위해 바로 전 Authorize의 uuid를 담음
    private int connectorId;    // Connector ID
    private String idTag;       // ID Tag;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    @Override
    public boolean validate() { return true; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorizeData that = (AuthorizeData) o;
        return connectorId == that.connectorId &&
                Objects.equals(uuid, that.uuid) &&
                Objects.equals(idTag, that.idTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, connectorId, idTag);
    }
}
