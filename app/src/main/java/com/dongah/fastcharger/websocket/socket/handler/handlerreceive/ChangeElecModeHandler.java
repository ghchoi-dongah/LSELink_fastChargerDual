package com.dongah.fastcharger.websocket.socket.handler.handlerreceive;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.sqlite.SQLiteHelper;
import com.dongah.fastcharger.sqlite.dto.CpChgElecmode;
import com.dongah.fastcharger.utils.FileManagement;
import com.dongah.fastcharger.websocket.ocpp.core.DataTransferStatus;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.ChangeElecModeConfirm;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;
import com.dongah.fastcharger.websocket.socket.OcppHandler;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.ChangeElecModeThread;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ChangeElecModeHandler implements OcppHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChangeElecModeHandler.class);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        try {
            String vendorId = payload.getString("vendorId");
            String msgId = payload.getString("messageId");  //changeelecmode.req
            String dataStr = payload.getString("data");

            // DB update
            if (connectorId == 0) {
                for (int i = 1; i <= GlobalVariables.maxChannel; i++) {
                    updateChgElecMode(dataStr, i);
                }
            } else {
                updateChgElecMode(dataStr, connectorId);
            }

            // 파일 저장
            saveChangeElecModeToFile(dataStr);
            // 응답
            sendResponse(connectorId, messageId);
            // 출력 제한
            ChangeElecModeThread.processRechgElec(connectorId);
        } catch (Exception e) {
            logger.error("ChangeElecModeHandler error : {}", e.getMessage(), e);
        }
    }

    /**
     * data JSON을 파싱하여 CP_CHG_ELECMODE 테이블을 업데이트한다.
     * - connectorId == 0 : CONNECTOR_ID 1, 2 모두 업데이트
     * - connectorId == 1 또는 2 : 해당 CONNECTOR_ID만 업데이트
     */
    @SuppressLint("DefaultLocale")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateChgElecMode(String dataStr, int connectorId) {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            SQLiteHelper helper = SQLiteHelper.getInstance(activity);
            SQLiteDatabase sqLiteDatabase = helper.getWritableDatabase();
            String tableName = new CpChgElecmode().getTableName();

            JSONObject dataJson = new JSONObject(dataStr);

            // 테이블 없으면 테이블 생성 후 업데이트 진행
            if (!helper.isTableExists(helper, tableName)) {
                logger.warn("updateChgElecMode table not exists : {}", tableName);
                helper.onCreateTable(sqLiteDatabase, tableName);
                insertChgElecMode(helper, connectorId, dataJson);
                return;
            }

            // connector_id 존재 유무 확인
            Cursor cursor = helper.select(tableName,"CONNECTOR_ID=?", new String[]{String.valueOf(connectorId)});
            // Cursor null 여부 확인, 조회 결과 존재 여부 확인
            if (cursor == null || !cursor.moveToFirst()) {
                logger.warn("updateChgElecMode {} cursor is null or no data. connectorId : {}", tableName, connectorId);
                insertChgElecMode(helper, connectorId, dataJson);
                return;
            }

            // HH00 ~ HH23 값 추출
            ContentValues values = new ContentValues();
            for (int i = 0; i < 24; i++) {
                String key = String.format("HH%02d", i);
                if (dataJson.has(key)) {
                    values.put(key, dataJson.getInt(key));
                }
            }

            // MOD_DT 업데이트
            ZonedDateTimeConvert convert = new ZonedDateTimeConvert();
            values.put("MOD_DT", convert.doGetKstDatetimeAsString());

            // DB 업데이트
            if (connectorId == 0) {
                // connectorId가 0이면 CONNECTOR_ID 1, 2 모두 업데이트
                for (int id = 1; id <= GlobalVariables.maxChannel; id++) {
                    int updated = helper.update(tableName, values, "CONNECTOR_ID=?", new String[]{String.valueOf(id)});
                    logger.info("updateChgElecMode connectorId[{}] updated rows : {}", id, updated);
                }
            } else {
                // connectorId가 1 또는 2이면 해당 CONNECTOR_ID만 업데이트
                int updated = helper.update(tableName, values, "CONNECTOR_ID=?", new String[]{String.valueOf(connectorId)});
                logger.info("updateChgElecMode single connectorId[{}] updated rows : {}", connectorId, updated);
            }

            cursor.close();
        } catch (Exception e) {
            logger.error("updateChgElecMode error : {}", e.getMessage(), e);
        }
    }

    // CP_CHG_ELECMODE insert
    @SuppressLint("DefaultLocale")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void insertChgElecMode(SQLiteHelper sqLiteHelper, int connectorId, JSONObject dataJson) {
        try {
            CpChgElecmode cpChgElecmode = new CpChgElecmode();
            cpChgElecmode.connectorId = connectorId;

            for (int time = 0; time < 24; time++) {
                String key = String.format("HH%02d", time);
                if (dataJson.has(key)) {
                    cpChgElecmode.hhXX[time] = dataJson.getInt(key);
                }
            }

            ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();
            cpChgElecmode.regDt = zonedDateTimeConvert.doGetKstDatetimeAsString();
            cpChgElecmode.modDt = zonedDateTimeConvert.doGetKstDatetimeAsString();

            // insert
            sqLiteHelper.insert(cpChgElecmode);
        } catch (Exception e) {
            logger.error("insertChgElecMode error : {}", e.getMessage(), e);
        }
    }

    private void saveChangeElecModeToFile(String newData) {
        try {
            FileManagement fileManagement = new FileManagement();
            JSONObject rootJson;

            File file = new File(GlobalVariables.getRootPath() + File.separator + "changeElecMode");

            if (!file.exists()) {
                rootJson = new JSONObject();
            } else {
                String oldText = readFile(file);
                if (oldText == null || oldText.isEmpty()) {
                    rootJson = new JSONObject();
                } else {
                    rootJson = new JSONObject(oldText);
                }
            }
            JSONObject newJson = new JSONObject(newData);
            int connectorId = newJson.getInt("connectorId");
            rootJson.put(String.valueOf(connectorId), newJson);

            fileManagement.stringToFileSave(
                    GlobalVariables.getRootPath(),
                    "changeElecMode",
                    rootJson.toString(),
                    false);
        } catch (Exception e) {
            logger.error("saveChangeElecModeToFile error : {}", e.getMessage());
        }
    }

    private String readFile(File file) throws Exception {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        bufferedReader.close();
        return stringBuilder.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendResponse(int connectorId, String messageId) {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;

            ChangeElecModeConfirm changeElecModeConfirm = new ChangeElecModeConfirm();
            changeElecModeConfirm.setStatus(DataTransferStatus.Accepted);

            activity.getSocketReceiveMessage().onResultSend(
                    connectorId,
                    changeElecModeConfirm.getActionName(),
                    messageId,
                    changeElecModeConfirm);
        } catch (Exception e) {
            logger.error("sendResponse error : {}", e.getMessage());
        }
    }
}
