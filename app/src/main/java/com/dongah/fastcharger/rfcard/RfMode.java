package com.dongah.fastcharger.rfcard;

public enum RfMode {
    RF_CARD_RELEASE(0),
    RF_CARD_VER(1),
    RF_CARD_STATUS(2),
    RF_CARD_SCAN(3),
    RF_CARD_CONTINUE_TMONEY(4);

    private final int value;

    RfMode (int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}
