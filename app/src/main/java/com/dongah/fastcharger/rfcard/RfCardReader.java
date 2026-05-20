package com.dongah.fastcharger.rfcard;

public abstract class RfCardReader {
    protected RfCardReaderListener rfCardReaderListener = null;

    public void setRfCardReaderListener(RfCardReaderListener rfCardReaderListener) {
        this.rfCardReaderListener = rfCardReaderListener;
    }

    public abstract void rfCardReadRequest(int ch);
    public abstract void rfCardReadRelease();
    public abstract void destroyThread();
    public abstract void stopThread();
}
