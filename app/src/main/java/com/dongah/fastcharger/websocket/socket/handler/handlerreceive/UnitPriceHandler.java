package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.sqlite.SQLiteHelper;
import com.dongah.fastcharger.sqlite.dto.CpUnitPrice;
import com.dongah.fastcharger.utils.FileManagement;
import com.dongah.fastcharger.websocket.ocpp.core.DataTransferStatus;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;
import com.dongah.fastcharger.websocket.socket.OcppHandler;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnitPriceHandler implements OcppHandler  {
    private static final Logger logger = LoggerFactory.getLogger(UnitPriceHandler.class);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        DataTransferStatus status = DataTransferStatus.valueOf(payload.getString("status"));
        String dataStr = payload.getString("data");

        if (status.equals(DataTransferStatus.Accepted)) {
            // 저장
            FileManagement fileManagement = new FileManagement();
            fileManagement.stringToFileSave(GlobalVariables.getRootPath(), "unitPrice", dataStr, false);

            /* DB update */
            if (connectorId == 0 || connectorId == 100) {
                for (int i = 1; i <= GlobalVariables.maxChannel; i++) {
                    updateUnitPrice(dataStr, i);
                }
            } else {
                updateUnitPrice(dataStr, connectorId);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateUnitPrice(String dataStr, int connectorId) {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            SQLiteHelper helper = SQLiteHelper.getInstance(activity);
            SQLiteDatabase sqLiteDatabase = helper.getWritableDatabase();
            String tableName = new CpUnitPrice().getTableName();

            JSONArray dataArr = new JSONArray(dataStr);

            helper.dropTable(sqLiteDatabase, tableName);
            // 테이블이 없으면 테이블 생성 후 insertUnitPrice
            if (!helper.isTableExists(helper, tableName)) {
                logger.warn("updateUnitPrice table not exists : {}", tableName);
                helper.onCreateTable(sqLiteDatabase, tableName);
                insertUnitPrice(helper, dataStr, connectorId);
                return;
            }

            // 테이블이 있으면 row 단위로 update or insert
            for (int i = 0; i < dataArr.length(); i++) {
                JSONObject row = dataArr.getJSONObject(i);
                double unitPrice     = row.getDouble("UnitPrice");
                String userTypeCd    = row.getString("UserTypeCd");
                double crtrUnitPrice = row.getDouble("CrtrUnitPrice");
                String rechgType     = row.getString("RechgType");

                Cursor cursor = null;
                boolean exists = false;
                try {
                    cursor = sqLiteDatabase.rawQuery(
                            "SELECT 1 FROM " + tableName +
                                    " WHERE connector_id = ? AND USER_TYPE_CD = ?",
                            new String[]{ String.valueOf(connectorId), userTypeCd });
                    exists = (cursor != null && cursor.moveToFirst());
                } finally {
                    if (cursor != null) cursor.close();
                }

                ContentValues cv = new ContentValues();
                cv.put("UnitPrice", unitPrice);
                cv.put("CrtrUnitPrice", crtrUnitPrice);
                cv.put("RechgType", rechgType);
                ZonedDateTimeConvert convert = new ZonedDateTimeConvert();
                cv.put("REG_DT", convert.doGetKstDatetimeAsString());


                if (exists) {
                    int updated = sqLiteDatabase.update(tableName, cv,
                            "CONNECTOR_ID = ? AND USER_TYPE_CD = ?",
                            new String[]{ String.valueOf(connectorId), userTypeCd });
                    logger.info("updateUnitPrice updated: connectorId={}, userTypeCd={}, rows={}",
                            connectorId, userTypeCd, updated);
                } else {
                    cv.put("CONNECTOR_ID", connectorId);
                    cv.put("USER_TYPE_CD", userTypeCd);
                    long id = sqLiteDatabase.insert(tableName, null, cv);
                    logger.info("updateUnitPrice inserted: connectorId={}, userTypeCd={}, id={}",
                            connectorId, userTypeCd, id);
                }
                setUnitPriceCd(userTypeCd, unitPrice);
            }
        } catch (Exception e) {
            logger.error("updateUnitPrice error : {}", e.getMessage(), e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void insertUnitPrice(SQLiteHelper sqLiteHelper, String dataStr, int connectorId) {
        try {
            SQLiteDatabase db = sqLiteHelper.getWritableDatabase();
            String tableName = new CpUnitPrice().getTableName();

            JSONArray dataArr = new JSONArray(dataStr);
            for (int i = 0; i < dataArr.length(); i++) {
                JSONObject row = dataArr.getJSONObject(i);

                ContentValues cv = new ContentValues();
                cv.put("CONNECTOR_ID", connectorId);
                cv.put("UNIT_PRICE",     row.getDouble("UnitPrice"));
                cv.put("USER_TYPE_CD",    row.getString("UserTypeCd"));
                cv.put("CRTR_UNIT_PRICE", row.getDouble("CrtrUnitPrice"));
                cv.put("RE_CHG_TYPE",     row.getString("RechgType"));

                ZonedDateTimeConvert convert = new ZonedDateTimeConvert();
                cv.put("REG_DT", convert.doGetKstDatetimeAsString());

                setUnitPriceCd(row.getString("UserTypeCd"), row.getDouble("UnitPrice"));

                long id = db.insert(tableName, null, cv);
                logger.info("insertUnitPrice inserted: connectorId={}, userTypeCd={}, id={}",
                        connectorId, row.getString("UserTypeCd"), id);
            }
        } catch (Exception e) {
            logger.error("insertUnitPrice error : {}", e.getMessage(), e);
        }
    }

    // 회원별 단가 정보 설정
    private void setUnitPriceCd(String userTypeCd, double unitPrice) {
        switch (userTypeCd) {
            case "C":
                GlobalVariables.userTypeC = unitPrice;
                break;
            case "K":
                GlobalVariables.userTypeK = unitPrice;
                break;
            case "M":
                GlobalVariables.userTypeM = unitPrice;
                break;
            case "N":
                GlobalVariables.userTypeN = unitPrice;
                break;
        }
    }
}
