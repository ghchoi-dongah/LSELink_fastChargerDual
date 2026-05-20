package com.dongah.fastcharger.websocket.ocpp.core.datatransfer.vas;

import androidx.annotation.NonNull;

import com.dongah.fastcharger.websocket.ocpp.common.model.Request;
import com.dongah.fastcharger.websocket.ocpp.utilities.MoreObjects;

import java.util.Objects;

public class VasRequest implements Request {

    private static final String TAG = VasRequest.class.getSimpleName();
    private static final String ACTION_NAME = "DataTransfer";


    private String vendorId;
    private String messageId;
    private VasData data;

    public VasRequest() {
    }

    public String getActionName() {
        return ACTION_NAME;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        this.vendorId = vendorId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public VasData getData() {
        return data;
    }

    public void setData(VasData data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VasRequest that = (VasRequest) o;
        return Objects.equals(vendorId, that.vendorId) && Objects.equals(messageId, that.messageId) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendorId, messageId, data);
    }

    @Override
    public boolean transactionRelated() {
        return false;
    }

    @Override
    public boolean validate() {
        return true;
    }

    @NonNull
    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("vendorId", vendorId)
                .add("messageId", messageId)
                .add("isValid", validate())
                .toString();
    }
}
