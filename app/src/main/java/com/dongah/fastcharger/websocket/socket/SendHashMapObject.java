package com.dongah.fastcharger.websocket.socket;

public class SendHashMapObject {
    private int connectorId;
    private String actionName;
    private boolean dataTransfer;

    public SendHashMapObject() {
    }

    public SendHashMapObject(int connectorId, String actionName) {
        this.connectorId = connectorId;
        this.actionName = actionName;
    }

    public int getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(int connectorId) {
        this.connectorId = connectorId;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public boolean isDataTransfer() {
        return dataTransfer;
    }

    public void setDataTransfer(boolean dataTransfer) {
        this.dataTransfer = dataTransfer;
    }
}
