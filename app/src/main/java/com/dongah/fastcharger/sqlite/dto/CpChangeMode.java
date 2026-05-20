package com.dongah.fastcharger.sqlite.dto;

import android.annotation.SuppressLint;
import android.content.ContentValues;

public class CpChangeMode implements DbEntity {
    private static final String tableName = "CP_CHANGE_MODE";
    private static final String ID = "ID";
    private static final String CONNECTOR_ID = "CONNECTOR_ID";
    private static final String RECHG_AMT = "RECHG_AMT";
    private static final String RECHG_ELEC = "RECHG_ELEC";
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
                    RECHG_AMT + " INTEGER NOT NULL," +
                    RECHG_ELEC + " INTEGER NOT NULL," +
                    HH00 + " TEXT NOT NULL," +
                    HH01 + " TEXT NOT NULL," +
                    HH02 + " TEXT NOT NULL," +
                    HH03 + " TEXT NOT NULL," +
                    HH04 + " TEXT NOT NULL," +
                    HH05 + " TEXT NOT NULL," +
                    HH06 + " TEXT NOT NULL," +
                    HH07 + " TEXT NOT NULL," +
                    HH08 + " TEXT NOT NULL," +
                    HH09 + " TEXT NOT NULL," +
                    HH10 + " TEXT NOT NULL," +
                    HH11 + " TEXT NOT NULL," +
                    HH12 + " TEXT NOT NULL," +
                    HH13 + " TEXT NOT NULL," +
                    HH14 + " TEXT NOT NULL," +
                    HH15 + " TEXT NOT NULL," +
                    HH16 + " TEXT NOT NULL," +
                    HH17 + " TEXT NOT NULL," +
                    HH18 + " TEXT NOT NULL," +
                    HH19 + " TEXT NOT NULL," +
                    HH20 + " TEXT NOT NULL," +
                    HH21 + " TEXT NOT NULL," +
                    HH22 + " TEXT NOT NULL," +
                    HH23 + " TEXT NOT NULL," +
                    REG_DT  + " TEXT NOT NULL," +
                    MOD_DT  + " TEXT NULL" +
                    ");";



    public Integer connectorId;
    public Integer rechgAmt;
    public Integer rechgElec;
    public String[] hhXX = new String[24];
    public String regDt;
    public String modDt;

    public CpChangeMode() {}

    @Override
    public String getTableName() {
        return tableName;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(CONNECTOR_ID, connectorId);
        values.put(RECHG_AMT, rechgAmt);
        values.put(RECHG_ELEC, rechgElec);
        for (int i = 0; i < 24; i++) {
            values.put(String.format("HH%02d", i), hhXX[i]);
        }
        values.put(REG_DT, regDt);
        values.put(MOD_DT, modDt);
        return values;
    }
}
