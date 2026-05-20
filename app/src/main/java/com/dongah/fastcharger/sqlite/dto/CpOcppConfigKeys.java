package com.dongah.fastcharger.sqlite.dto;

import android.content.ContentValues;

public class CpOcppConfigKeys implements DbEntity {
    private static final String tableName = "CP_OCPP_CONFIGKEYS";
    private static final String ID = "ID";
    private static final String KEY = "CONFIG_KEY";
    private static final String VALUE = "CONFIG_VALUE";
    private static final String READ_ONLY = "READ_ONLY";
    private static final String REG_DT = "REG_DT";
    public static final String CREATE_SQL =
            "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    KEY + " TEXT NOT NULL," +
                    VALUE + " TEXT NOT NULL," +
                    READ_ONLY + " INTEGER NOT NULL," +
                    REG_DT  + " TEXT NOT NULL" +
                    ");";
            
    public String key;
    public String value;
    public Integer readOnly;
    public String regDt;

    public CpOcppConfigKeys() {}
    
    @Override
    public String getTableName() {
        return tableName;
    }
    
    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(KEY, key);
        values.put(VALUE, value);
        values.put(READ_ONLY, readOnly);
        values.put(REG_DT, regDt);
        return values;
    }
}
