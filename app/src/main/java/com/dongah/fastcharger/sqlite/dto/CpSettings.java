package com.dongah.fastcharger.sqlite.dto;

import android.content.ContentValues;

public class CpSettings implements DbEntity {
    private static final String tableName = "CP_SETTINGS";
    private static final String ID = "ID";
    private static final String STATION_ID = "STATION_ID";
    private static final String CHARGER_ID = "CHARGER_ID";
    private static final String MODEL_NM = "MODEL_NM";
    private static final String VENDOR_NM = "VENDOR_NM";
    private static final String FW_VERSION = "FW_VERSION";
    private static final String SOC_LIMIT = "SOC_LIMIT";
    private static final String AVAILABILITY = "AVAILABILITY";
    public static final String CREATE_SQL =
            "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    STATION_ID + " TEXT NOT NULL," +
                    CHARGER_ID + " TEXT NOT NULL," +
                    MODEL_NM + " TEXT NOT NULL," +
                    VENDOR_NM + " TEXT NOT NULL," +
                    FW_VERSION + " TEXT NOT NULL," +
                    SOC_LIMIT + " TEXT," +
                    AVAILABILITY + " TEXT NOT NULL" +
                    ");";

    public String stationId;
    public String chargerId;
    public String modelNm;
    public String vendorNm;
    public String fwVersion;;
    public String socLimit;
    public String availability;

    public CpSettings() {
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(STATION_ID, stationId);
        values.put(CHARGER_ID, chargerId);
        values.put(MODEL_NM, modelNm);
        values.put(VENDOR_NM, vendorNm);
        values.put(FW_VERSION, fwVersion);
        values.put(SOC_LIMIT, socLimit);
        values.put(AVAILABILITY, availability);
        return values;
    }

}
