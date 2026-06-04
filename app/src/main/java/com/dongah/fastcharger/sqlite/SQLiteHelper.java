package com.dongah.fastcharger.sqlite;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.dongah.fastcharger.sqlite.dto.CpChangeMode;
import com.dongah.fastcharger.sqlite.dto.CpChargingHist;
import com.dongah.fastcharger.sqlite.dto.CpChgElecmode;
import com.dongah.fastcharger.sqlite.dto.CpNonTransmit;
import com.dongah.fastcharger.sqlite.dto.CpOcppConfigKeys;
import com.dongah.fastcharger.sqlite.dto.CpRechgSoc;
import com.dongah.fastcharger.sqlite.dto.CpSettings;
import com.dongah.fastcharger.sqlite.dto.CpUnitPrice;
import com.dongah.fastcharger.sqlite.dto.DbEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLiteHelper extends SQLiteOpenHelper {
    private static final Logger logger = LoggerFactory.getLogger(SQLiteHelper.class);

    private static final String DATABASE_NAME = "dongah.db";
    private static final  int DATABASE_VERSION = 1;

    private static SQLiteHelper instance;

    public static SQLiteHelper getInstance(Context context) {
        if (instance == null) {
            instance = new SQLiteHelper(context.getApplicationContext());
        }
        return instance;
    }

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        /** SQLite Type
         * NULL: NULL값
         * INTEGER: 정수형(boolean: 1 or 0)
         * REAL: 실수형
         * TEXT: 문자열
         * BLOB: 바이너리
         * */
        sqLiteDatabase.execSQL(CpChgElecmode.CREATE_SQL);
        sqLiteDatabase.execSQL(CpChangeMode.CREATE_SQL);
        sqLiteDatabase.execSQL(CpRechgSoc.CREATE_SQL);
        sqLiteDatabase.execSQL(CpUnitPrice.CREATE_SQL);

//        sqLiteDatabase.execSQL(CpSettings.CREATE_SQL);
//        sqLiteDatabase.execSQL(CpOcppConfigKeys.CREATE_SQL);
//        sqLiteDatabase.execSQL(CpNonTransmit.CREATE_SQL);
//        sqLiteDatabase.execSQL(CpChargingHist.CREATE_SQL);
    }

    public void onCreateTable(SQLiteDatabase sqLiteDatabase, String tableName) {
        switch (tableName) {
            case "CP_CHG_ELECMODE":
                sqLiteDatabase.execSQL(CpChgElecmode.CREATE_SQL);
                break;
            case "CP_CHANGE_MODE":
                sqLiteDatabase.execSQL(CpChangeMode.CREATE_SQL);
                break;
            case "CP_RECHG_SOC":
                sqLiteDatabase.execSQL(CpRechgSoc.CREATE_SQL);
                break;
            case "CP_UNIT_PRICE":
                sqLiteDatabase.execSQL(CpUnitPrice.CREATE_SQL);
                break;
            default:
                throw new IllegalArgumentException("Unknown table: " + tableName);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        String sql = "DROP TABLE if exists mytable";
        sqLiteDatabase.execSQL(sql);
        onCreate(sqLiteDatabase);
    }

    // insert
    public long insert(DbEntity entity) {
        SQLiteDatabase db = getWritableDatabase();
        return db.insert(entity.getTableName(), null, entity.toContentValues());
    }

    // delete all tables
    public void dropAllTables(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + new CpSettings().getTableName());
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + new CpOcppConfigKeys().getTableName());
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + new CpUnitPrice().getTableName());
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + new CpChgElecmode().getTableName());
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + new CpChangeMode().getTableName());
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + new CpNonTransmit().getTableName());
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + new CpChargingHist().getTableName());
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + new CpRechgSoc().getTableName());
    }

    // delete table
    public void dropTable(SQLiteDatabase sqLiteDatabase, String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) return;
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + tableName);
    }

    // update
    public int update(String tableName, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = getWritableDatabase();
        return db.update(tableName, values, where, whereArgs);
    }


    // delete with where
    public int delete(String tableName, String where, String[] whereArgs) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(tableName, where, whereArgs);
    }

    // delete all (테이블의 모든 데이터 삭제, 테이블 삭제X)
    public int deleteAll(String tableName) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(tableName, null, null);
    }

    // select (전체 조회)
    public Cursor selectAll(String tableName) {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(
                tableName,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    // select with where
    public Cursor select(String tableName, String where, String[] whereArgs) {
        SQLiteDatabase db = getReadableDatabase();
        return db.query(
                tableName,
                null,
                where,
                whereArgs,
                null,
                null,
                null
        );
    }

    /**
     * SQLite에 특정 테이블이 존재하는지 확인한다.
     *
     * @param helper SQLiteHelper 인스턴스
     * @param tableName 확인할 테이블명
     * @return 테이블이 존재하면 true, 없으면 false
     */
    public boolean isTableExists(SQLiteHelper helper, String tableName) {
        Cursor cursor = null;

        try {
            /*
             * sqlite_master는 SQLite 내부 메타 테이블.
             * type='table'이고 name이 tableName인 데이터가 있으면 해당 테이블이 존재함.
             */
            cursor = helper.getReadableDatabase().rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                    new String[]{tableName}
            );

            return cursor != null && cursor.moveToFirst();

        } catch (Exception e) {
            logger.error("isTableExists error. tableName : {}, error : {}", tableName, e.getMessage(), e);
            return false;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
