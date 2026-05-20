package com.dongah.fastcharger.rfcard;

public interface RfCardReaderListener {
    void onRfCardDataReceive(int ch, String cardNum, boolean value);
}
