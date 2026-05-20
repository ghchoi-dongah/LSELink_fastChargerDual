package com.dongah.fastcharger.sqlite.dto;

import android.annotation.SuppressLint;
import android.content.ContentValues;

public class CpChgElecmode implements DbEntity {
    private static final String tableName = "CP_CHG_ELECMODE";
    private static final String ID = "ID";
    private static final String CONNECTOR_ID = "CONNECTOR_ID";
    private static final String HH00 = "HH00";
    private static final String HH01 = "HH01";
    private static final String HH02 = "HH02";
    private static final String HH03 = "HH03";
    private static final String HH04 = "HH04";
    private static final String HH05 = "HH05";
    private static final String HH06 = "HH06";
    private static final String HH07 = "HH07";
    private static final String HH08 = "HH08";
    private static final String HH09 = "HH09";
    private static final String HH10 = "HH10";
    private static final String HH11 = "HH11";
    private static final String HH12 = "HH12";
    private static final String HH13 = "HH13";
    private static final String HH14 = "HH14";
    private static final String HH15 = "HH15";
    private static final String HH16 = "HH16";
    private static final String HH17 = "HH17";
    private static final String HH18 = "HH18";
    private static final String HH19 = "HH19";
    private static final String HH20 = "HH20";
    private static final String HH21 = "HH21";
    private static final String HH22 = "HH22";
    private static final String HH23 = "HH23";
    private static final String REG_DT = "REG_DT";
    private static final String MOD_DT = "MOD_DT";
    public static final String CREATE_SQL =
            "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    CONNECTOR_ID + " INTEGER NOT NULL," +
                    HH00 + " INTEGER NOT NULL," +
                    HH01 + " INTEGER NOT NULL," +
                    HH02 + " INTEGER NOT NULL," +
                    HH03 + " INTEGER NOT NULL," +
                    HH04 + " INTEGER NOT NULL," +
                    HH05 + " INTEGER NOT NULL," +
                    HH06 + " INTEGER NOT NULL," +
                    HH07 + " INTEGER NOT NULL," +
                    HH08 + " INTEGER NOT NULL," +
                    HH09 + " INTEGER NOT NULL," +
                    HH10 + " INTEGER NOT NULL," +
                    HH11 + " INTEGER NOT NULL," +
                    HH12 + " INTEGER NOT NULL," +
                    HH13 + " INTEGER NOT NULL," +
                    HH14 + " INTEGER NOT NULL," +
                    HH15 + " INTEGER NOT NULL," +
                    HH16 + " INTEGER NOT NULL," +
                    HH17 + " INTEGER NOT NULL," +
                    HH18 + " INTEGER NOT NULL," +
                    HH19 + " INTEGER NOT NULL," +
                    HH20 + " INTEGER NOT NULL," +
                    HH21 + " INTEGER NOT NULL," +
                    HH22 + " INTEGER NOT NULL," +
                    HH23 + " INTEGER NOT NULL," +
                    REG_DT  + " TEXT NOT NULL," +
                    MOD_DT  + " TEXT NULL" +
                    ");";


    public Integer connectorId;
    public Integer[] hhXX = new Integer[24];
    public String regDt;
    public String modDt;

    public CpChgElecmode() {}

    @Override
    public String getTableName() {
        return tableName;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();

        values.put(CONNECTOR_ID, connectorId);
        for (int i = 0; i < 24; i++) {
            values.put(String.format("HH%02d", i), hhXX[i]);
        }
        values.put(REG_DT, regDt);
        values.put(MOD_DT, modDt);
        return values;
    }
}
