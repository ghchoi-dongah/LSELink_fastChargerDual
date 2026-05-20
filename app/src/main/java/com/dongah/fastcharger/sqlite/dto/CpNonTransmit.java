package com.dongah.fastcharger.sqlite.dto;

import android.content.ContentValues;

public class CpNonTransmit implements DbEntity {
    private static final String tableName = "CP_NON_TRANSMIT";
    private static final String ID = "ID";
    private static final String MESSAGE = "MESSAGE";
    private static final String REG_DT = "REG_DT";
    private static final String SEND_DT = "SEND_DT";
    private static final String RETRANSMITT_YN = "RETRANSMITT_YN";
    public static final String CREATE_SQL =
            "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    MESSAGE + " MESSAGE NOT NULL," +
                    REG_DT + " TEXT," +
                    SEND_DT + " TEXT," +
                    RETRANSMITT_YN  + " TEXT NOT NULL DEFAULT 'N'" +
                    ");";

    public String message;
    public String regDt;
    public String sendDt;
    public String retransmitYn;

    public CpNonTransmit() {}

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(message, MESSAGE);
        values.put(REG_DT, regDt);
        values.put(sendDt, SEND_DT);
        values.put(RETRANSMITT_YN, retransmitYn);
        return values;
    }
}
