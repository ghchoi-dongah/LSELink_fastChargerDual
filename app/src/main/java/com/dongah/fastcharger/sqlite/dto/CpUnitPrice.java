package com.dongah.fastcharger.sqlite.dto;

import android.content.ContentValues;

public class CpUnitPrice implements DbEntity {
    private static final String tableName = "CP_UNIT_PRICE";
    private static final String ID = "ID";
    private static final String CONNECTOR_ID = "CONNECTOR_ID";
    private static final String UNIT_PRICE = "UNIT_PRICE";
    private static final String USER_TYPE_CD = "USER_TYPE_CD";
    private static final String CRTR_UNIT_PRICE = "CRTR_UNIT_PRICE";
    private static final String RE_CHG_TYPE = "RE_CHG_TYPE";
    private static final String REG_DT = "REG_DT";
    public static final String CREATE_SQL =
            "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    CONNECTOR_ID + " TEXT NOT NULL," +
                    UNIT_PRICE + " REAL NOT NULL," +
                    USER_TYPE_CD + " TEXT NOT NULL," +
                    CRTR_UNIT_PRICE + " REAL NOT NULL," +
                    RE_CHG_TYPE + " TEXT NOT NULL," +
                    REG_DT + " TEXT NOT NULL," +
                    "UNIQUE(" + CONNECTOR_ID + ", " + USER_TYPE_CD + ")" +
                    ");";

    public Integer connectorId;
    public Double unitPrice;
    public String userTypeCd;
    public Double crtrUnitPrice;
    public String reChgType;
    public String regDt;

    public CpUnitPrice() {}

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(CONNECTOR_ID, connectorId);
        values.put(UNIT_PRICE, unitPrice);
        values.put(USER_TYPE_CD, userTypeCd);
        values.put(CRTR_UNIT_PRICE, crtrUnitPrice);
        values.put(RE_CHG_TYPE, reChgType);
        values.put(REG_DT, regDt);
        return values;
    }
}
