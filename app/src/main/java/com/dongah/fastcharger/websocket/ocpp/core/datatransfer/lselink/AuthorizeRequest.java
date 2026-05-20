package com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink;

import androidx.annotation.NonNull;

import com.dongah.fastcharger.websocket.ocpp.common.PropertyConstraintException;
import com.dongah.fastcharger.websocket.ocpp.common.model.Request;
import com.dongah.fastcharger.websocket.ocpp.utilities.ModelUtil;
import com.dongah.fastcharger.websocket.ocpp.utilities.MoreObjects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class AuthorizeRequest implements Request {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizeRequest.class);

    private static final String ACTION_NAME = "DataTransfer";
    private static final int STRING_255_CHAR_MAX_LENGTH = 255;
    private static final int STRING_50_CHAR_MAX_LENGTH = 50;
    private static final String ERROR_MESSAGE = "Exceeded limit of %s chars";
    private String vendorId;
    private String messageId;
    private String data;

    public AuthorizeRequest() {}
    private static String validationErrorMessage(int maxAllowedLength) {
        return String.format(ERROR_MESSAGE, maxAllowedLength);
    }
    public String getActionName() {
        return ACTION_NAME;
    }

    public String getVendorId() {
        return vendorId;
    }

    public void setVendorId(String vendorId) {
        if (!ModelUtil.validate(vendorId, STRING_255_CHAR_MAX_LENGTH)) {
            throw new PropertyConstraintException(vendorId.length(), validationErrorMessage(STRING_255_CHAR_MAX_LENGTH));
        }
        this.vendorId = vendorId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        if (!ModelUtil.validate(messageId, STRING_50_CHAR_MAX_LENGTH)) {
            throw new PropertyConstraintException(messageId.length(), validationErrorMessage(STRING_50_CHAR_MAX_LENGTH));
        }
        this.messageId = messageId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public boolean transactionRelated() {
        return false;
    }

    @Override
    public boolean validate() {
        return ModelUtil.validate(vendorId, STRING_255_CHAR_MAX_LENGTH);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthorizeRequest that = (AuthorizeRequest) o;
        return Objects.equals(vendorId, that.vendorId) && Objects.equals(messageId, that.messageId) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendorId, messageId, data);
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
