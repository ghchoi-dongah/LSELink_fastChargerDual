package com.dongah.fastcharger.sqlite.dto;

import android.annotation.SuppressLint;
import android.content.ContentValues;

public class CpRechgSoc implements DbEntity {
    private static final String tableName = "CP_RECHG_SOC";
    private static final String ID = "ID";
    private static final String CONNECTOR_ID = "CONNECTOR_ID";
    private static final String DH00 = "DH00";
    private static final String DH01 = "DH01";
    private static final String DH02 = "DH02";
    private static final String DH03 = "DH03";
    private static final String DH04 = "DH04";
    private static final String DH05 = "DH05";
    private static final String DH06 = "DH06";
    private static final String DH07 = "DH07";
    private static final String DH08 = "DH08";
    private static final String DH09 = "DH09";
    private static final String DH10 = "DH10";
    private static final String DH11 = "DH11";
    private static final String DH12 = "DH12";
    private static final String DH13 = "DH13";
    private static final String DH14 = "DH14";
    private static final String DH15 = "DH15";
    private static final String DH16 = "DH16";
    private static final String DH17 = "DH17";
    private static final String DH18 = "DH18";
    private static final String DH19 = "DH19";
    private static final String DH20 = "DH20";
    private static final String DH21 = "DH21";
    private static final String DH22 = "DH22";
    private static final String DH23 = "DH23";
    private static final String WH00 = "WH00";
    private static final String WH01 = "WH01";
    private static final String WH02 = "WH02";
    private static final String WH03 = "WH03";
    private static final String WH04 = "WH04";
    private static final String WH05 = "WH05";
    private static final String WH06 = "WH06";
    private static final String WH07 = "WH07";
    private static final String WH08 = "WH08";
    private static final String WH09 = "WH09";
    private static final String WH10 = "WH10";
    private static final String WH11 = "WH11";
    private static final String WH12 = "WH12";
    private static final String WH13 = "WH13";
    private static final String WH14 = "WH14";
    private static final String WH15 = "WH15";
    private static final String WH16 = "WH16";
    private static final String WH17 = "WH17";
    private static final String WH18 = "WH18";
    private static final String WH19 = "WH19";
    private static final String WH20 = "WH20";
    private static final String WH21 = "WH21";
    private static final String WH22 = "WH22";
    private static final String WH23 = "WH23";
    private static final String REG_DT = "REG_DT";
    private static final String MOD_DT = "MOD_DT";

    public static final String CREATE_SQL =
            "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    CONNECTOR_ID + " INTEGER NOT NULL," +
                    DH00 + " INTEGER NOT NULL," +
                    DH01 + " INTEGER NOT NULL," +
                    DH02 + " INTEGER NOT NULL," +
                    DH03 + " INTEGER NOT NULL," +
                    DH04 + " INTEGER NOT NULL," +
                    DH05 + " INTEGER NOT NULL," +
                    DH06 + " INTEGER NOT NULL," +
                    DH07 + " INTEGER NOT NULL," +
                    DH08 + " INTEGER NOT NULL," +
                    DH09 + " INTEGER NOT NULL," +
                    DH10 + " INTEGER NOT NULL," +
                    DH11 + " INTEGER NOT NULL," +
                    DH12 + " INTEGER NOT NULL," +
                    DH13 + " INTEGER NOT NULL," +
                    DH14 + " INTEGER NOT NULL," +
                    DH15 + " INTEGER NOT NULL," +
                    DH16 + " INTEGER NOT NULL," +
                    DH17 + " INTEGER NOT NULL," +
                    DH18 + " INTEGER NOT NULL," +
                    DH19 + " INTEGER NOT NULL," +
                    DH20 + " INTEGER NOT NULL," +
                    DH21 + " INTEGER NOT NULL," +
                    DH22 + " INTEGER NOT NULL," +
                    DH23 + " INTEGER NOT NULL," +
                    WH00 + " INTEGER NOT NULL," +
                    WH01 + " INTEGER NOT NULL," +
                    WH02 + " INTEGER NOT NULL," +
                    WH03 + " INTEGER NOT NULL," +
                    WH04 + " INTEGER NOT NULL," +
                    WH05 + " INTEGER NOT NULL," +
                    WH06 + " INTEGER NOT NULL," +
                    WH07 + " INTEGER NOT NULL," +
                    WH08 + " INTEGER NOT NULL," +
                    WH09 + " INTEGER NOT NULL," +
                    WH10 + " INTEGER NOT NULL," +
                    WH11 + " INTEGER NOT NULL," +
                    WH12 + " INTEGER NOT NULL," +
                    WH13 + " INTEGER NOT NULL," +
                    WH14 + " INTEGER NOT NULL," +
                    WH15 + " INTEGER NOT NULL," +
                    WH16 + " INTEGER NOT NULL," +
                    WH17 + " INTEGER NOT NULL," +
                    WH18 + " INTEGER NOT NULL," +
                    WH19 + " INTEGER NOT NULL," +
                    WH20 + " INTEGER NOT NULL," +
                    WH21 + " INTEGER NOT NULL," +
                    WH22 + " INTEGER NOT NULL," +
                    WH23 + " INTEGER NOT NULL," +
                    REG_DT  + " TEXT NOT NULL," +
                    MOD_DT  + " TEXT NULL" +
                    ");";

    public Integer connectorId;
    public Integer[] dhXX = new Integer[24];
    public Integer[] whXX = new Integer[24];
    public String regDt;
    public String modDt;

    public CpRechgSoc() {}

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
            values.put(String.format("DH%02d", i), dhXX[i]);
            values.put(String.format("WH%02d", i), whXX[i]);
        }
        values.put(REG_DT, regDt);
        values.put(MOD_DT, modDt);
        return values;
    }
}
