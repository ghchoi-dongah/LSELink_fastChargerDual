package com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink;

import com.dongah.fastcharger.websocket.ocpp.common.model.Validatable;

import java.util.Objects;

public class BatteryEncryptKeyData implements Validatable {

    private String keyId;       // key ID
    private String encryptPub;  // 암호화 공개키
    private String signData;    // 서명 키
    private String validTime;   // 만료일자(ex: 202505222228)

    public BatteryEncryptKeyData() {}

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getEncryptPub() {
        return encryptPub;
    }

    public void setEncryptPub(String encryptPub) {
        this.encryptPub = encryptPub;
    }

    public String getSignData() {
        return signData;
    }

    public void setSignData(String signData) {
        this.signData = signData;
    }

    public String getValidTime() {
        return validTime;
    }

    public void setValidTime(String validTime) {
        this.validTime = validTime;
    }

    @Override
    public boolean validate() { return true; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatteryEncryptKeyData that = (BatteryEncryptKeyData) o;
        return Objects.equals(keyId, that.keyId) &&
                Objects.equals(encryptPub, that.encryptPub) &&
                Objects.equals(signData, that.signData) &&
                Objects.equals(validTime, that.validTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyId, encryptPub, signData, validTime);
    }
}
