package com.dongah.fastcharger.sqlite.dto;

import android.content.ContentValues;

public interface DbEntity {
    String getTableName();
    ContentValues toContentValues();
}
