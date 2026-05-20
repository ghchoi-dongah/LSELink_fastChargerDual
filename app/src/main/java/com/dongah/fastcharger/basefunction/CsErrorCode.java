package com.dongah.fastcharger.basefunction;

public enum CsErrorCode {
    EMERGENCY(1000),
    PLCCOMM(1001),
    POWERMETERCOMM(1002),
    CHARGERLEAK(1003),
    CARLEAK(1004),
    OUTOVR(1005),
    OUTOCR(1006),
    COUPLERTEMPSENSOR(1007),
    COUPLEROVT(1008);

    CsErrorCode(int value) { this.value = value; }
    private final int value;
    public int value() {return value;}
}
