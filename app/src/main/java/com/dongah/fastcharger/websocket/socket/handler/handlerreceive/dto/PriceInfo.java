package com.dongah.fastcharger.websocket.socket.handler.handlerreceive.dto;

public class PriceInfo {

    private double unitPrice;
    private String userTypeCd;
    private double crtrUnitPrice;
    private String rechgType;

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getUserTypeCd() {
        return userTypeCd;
    }

    public void setUserTypeCd(String userTypeCd) {
        this.userTypeCd = userTypeCd;
    }

    public double getCrtrUnitPrice() {
        return crtrUnitPrice;
    }

    public void setCrtrUnitPrice(double crtrUnitPrice) {
        this.crtrUnitPrice = crtrUnitPrice;
    }

    public String getRechgType() {
        return rechgType;
    }

    public void setRechgType(String rechgType) {
        this.rechgType = rechgType;
    }
}
