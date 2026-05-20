package com.dongah.fastcharger.sqlite.dto;

import android.content.ContentValues;

public class CpChargingHist implements DbEntity {
    private static final String tableName = "CP_CHARGING_HIST";
    private static final String ID = "ID";
    private static final String STATION_ID = "STATION_ID";
    private static final String CHARGER_ID = "CHARGER_ID";
    private static final String CONNECTOR_ID = "CONNECTOR_ID";
    private static final String CHG_START_TIME = "CHG_START_TIME";
    private static final String CHG_END_TIME = "CHG_END_TIME";
    private static final String IDTAG = "IDTAG";
    private static final String SOC = "SOC";
    private static final String APPLY_UNITPRICE_ID = "APPLY_UNITPRICE_ID";
    private static final String START_METER = "START_METER";
    private static final String END_METER = "END_METER";
    private static final String CHG_AMOUNT = "CHG_AMOUNT";
    private static final String TRANSACTION_ID = "TRANSACTION_ID";
    private static final String REG_DT = "REG_DT";
    public static final String CREATE_SQL =
            "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    STATION_ID + " TEXT NOT NULL," +
                    CHARGER_ID + " TEXT NOT NULL," +
                    CONNECTOR_ID + " INTEGER," +
                    CHG_START_TIME + " TEXT," +
                    CHG_END_TIME + " TEXT," +
                    IDTAG + " TEXT NOT NULL," +
                    SOC + " TEXT," +
                    APPLY_UNITPRICE_ID + " INTEGER NOT NULL," +
                    START_METER + " INTEGER," +
                    END_METER + " INTEGER," +
                    CHG_AMOUNT + " INTEGER NOT NULL," +
                    TRANSACTION_ID + " INTEGER," +
                    REG_DT  + " TEXT NOT NULL" +
                    ");";

    public String stationId;
    public String chargerId;
    public Integer connectorId;
    public String chgStartTime;
    public String chgEndTime;
    public String idTag;
    public String soc;
    public Integer applyUnitPriceId;
    public Integer startMeter;
    public Integer endMeter;
    public Integer chgAmount;
    public Integer transactionId;
    public String regDt;

    public CpChargingHist() {}

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(STATION_ID, stationId);
        values.put(CHARGER_ID, chargerId);
        values.put(CONNECTOR_ID, connectorId);
        values.put(CHG_START_TIME, chgStartTime);
        values.put(CHG_END_TIME, chgEndTime);
        values.put(IDTAG, idTag);
        values.put(SOC, soc);
        values.put(APPLY_UNITPRICE_ID, applyUnitPriceId);
        values.put(START_METER, startMeter);
        values.put(END_METER, endMeter);
        values.put(CHG_AMOUNT, chgAmount);
        values.put(TRANSACTION_ID, transactionId);
        values.put(REG_DT, regDt);
        return values;
    }
}
