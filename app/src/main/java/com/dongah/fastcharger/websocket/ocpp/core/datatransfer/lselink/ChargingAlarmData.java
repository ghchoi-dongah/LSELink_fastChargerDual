package com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink;

import com.dongah.fastcharger.websocket.ocpp.common.model.Validatable;

import java.util.Objects;

public class ChargingAlarmData implements Validatable {

    private int connectorId;    // Connector ID
    private int msgType;        // 전송 메시지 타입(1:충전시작, 2:충전률 90%도달, 3:충전종료)
    private int transactionId;  // Transaction ID
    private String idTag;       // ID Tag
    private String phoneNum;    // 알림을 전송할 전화번호

    public ChargingAlarmData() {}

    public int getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(int connectorId) {
        this.connectorId = connectorId;
    }

    public int getMsgType() {
        return msgType;
    }

    public void setMsgType(int msgType) {
        this.msgType = msgType;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public String getIdTag() {
        return idTag;
    }

    public void setIdTag(String idTag) {
        this.idTag = idTag;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    @Override
    public boolean validate() { return true; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChargingAlarmData that = (ChargingAlarmData) o;
        return connectorId == that.connectorId &&
                msgType == that.msgType &&
                transactionId == that.transactionId &&
                Objects.equals(idTag, that.idTag) &&
                Objects.equals(phoneNum, that.phoneNum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectorId, msgType, transactionId, idTag, phoneNum);
    }
}
