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
import com.dongah.fastcharger.sqlite.dto.CpRechgSoc;
import com.dongah.fastcharger.utils.FileManagement;
import com.dongah.fastcharger.websocket.ocpp.core.DataTransferStatus;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.RechgrsocscheduleConfirm;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;
import com.dongah.fastcharger.websocket.socket.OcppHandler;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.RechgrsocscheduleThread;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


public class RechgrsocscheduleHandler implements OcppHandler {
    private static final Logger logger = LoggerFactory.getLogger(RechgrsocscheduleHandler.class);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        try {
            String vendorId = payload.getString("vendorId");
            String msgId = payload.getString("messageId");
            String dataStr = payload.getString("data");

            // DB update
            if (connectorId == 0) {
                for (int i = 1; i <= GlobalVariables.maxChannel; i++) {
                    updateRechgSoc(dataStr, i);
                }
            } else {
                updateRechgSoc(dataStr, connectorId);
            }

            // file save
            saveRechgrsocscheduleToFile(dataStr);
            // response
            sendResponse(connectorId, messageId);
            // soc 설정
            RechgrsocscheduleThread.processRechgSoc(connectorId);
        } catch (Exception e) {
            logger.error("RechgrsocscheduleHandler error : {}", e.getMessage(), e);
        }
    }

    /**
     * data JSON을 파싱하여 CP_RECHG_SOC 테이블을 업데이트한다.
     * - connectorId == 0 : CONNECTOR_ID 1, 2 모두 업데이트
     * - connectorId == 1 또는 2 : 해당 CONNECTOR_ID만 업데이트
     */
    @SuppressLint("DefaultLocale")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateRechgSoc(String dataStr, int connectorId) {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            SQLiteHelper helper = SQLiteHelper.getInstance(activity);
            SQLiteDatabase sqLiteDatabase = helper.getWritableDatabase();
            String tableName = new CpRechgSoc().getTableName();

            JSONObject dataJson = new JSONObject(dataStr);

            // 테이블 없으면 테이블 생성 후 업데이트 진행
            if (!helper.isTableExists(helper, tableName)) {
                logger.warn("updateRechgSoc table not exists : {}", tableName);
                helper.onCreateTable(sqLiteDatabase, tableName);
                insertRechgSoc(helper, connectorId, dataJson);
                return;
            }

            // connector_id 존재 유무 확인
            Cursor cursor = helper.select(tableName,"CONNECTOR_ID=?", new String[]{String.valueOf(connectorId)});
            // Cursor null 여부 확인, 조회 결과 존재 여부 확인
            if (cursor == null || !cursor.moveToFirst()) {
                logger.warn("updateRechgSoc {} cursor is null or no data. connectorId : {}", tableName, connectorId);
                insertRechgSoc(helper, connectorId, dataJson);
                return;
            }

            // DH00 ~ DH23, WH00 ~ WH23 값 추출
            ContentValues values = new ContentValues();
            for (int i = 0; i < 24; i++) {
                String keyD = String.format("DH%02d", i);
                String keyW = String.format("WH%02d", i);

                if (dataJson.has(keyD)) {
                    values.put(keyD, dataJson.getString(keyD));
                } else if (dataJson.has(keyW)) {
                    values.put(keyW, dataJson.getString(keyW));
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
                    logger.info("updateRechgSoc connectorId[{}] updated rows : {}", id, updated);
                }
            } else {
                // connectorId가 1 또는 2이면 해당 CONNECTOR_ID만 업데이트
                int updated = helper.update(tableName, values, "CONNECTOR_ID=?", new String[]{String.valueOf(connectorId)});
                logger.info("updateRechgSoc single connectorId[{}] updated rows : {}", connectorId, updated);
            }

            cursor.close();
        } catch (Exception e) {
            logger.error("updateRechgSoc error : {}", e.getMessage(), e);
        }
    }

    // CP_RECHG_SOC insert
    @SuppressLint("DefaultLocale")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void insertRechgSoc(SQLiteHelper sqLiteHelper, int connectorId, JSONObject dataJson) {
        try {
            CpRechgSoc cpRechgSoc = new CpRechgSoc();
            cpRechgSoc.connectorId = connectorId;

            for (int time = 0; time < 24; time++) {
                String dKey = String.format("DH%02d", time);
                String wKey = String.format("WH%02d", time);
                if (dataJson.has(dKey)) {
                    cpRechgSoc.dhXX[time] = dataJson.getInt(dKey);
                }
                if (dataJson.has(wKey)) {
                    cpRechgSoc.whXX[time] = dataJson.getInt(wKey);
                }
            }

            ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();
            cpRechgSoc.regDt = zonedDateTimeConvert.doGetKstDatetimeAsString();
            cpRechgSoc.modDt = zonedDateTimeConvert.doGetKstDatetimeAsString();

            sqLiteHelper.insert(cpRechgSoc);
        } catch (Exception e) {
            logger.error("insertRechgSoc error : {}", e.getMessage(), e);
        }
    }

    private void saveRechgrsocscheduleToFile(String newData) {
        try {
            FileManagement fileManagement = new FileManagement();
            JSONObject rootJson;

            File file = new File(GlobalVariables.getRootPath() + File.separator + "rechgrsocschedule");

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

            // connectorId == 0 이면 전체 적용
            if (connectorId == 0) {
                // 0번 자체도 저장
                JSONObject connector0Json = new JSONObject(newJson.toString());
                connector0Json.put("connectorId", 0);
                rootJson.put("0", connector0Json);

                // 실제 커넥터 1 ~ maxChannel까지 저장
                for (int i = 1; i <= GlobalVariables.maxChannel; i++) {
                    JSONObject copiedJson = new JSONObject(newJson.toString());

                    // 저장되는 내부 connectorId를 실제 커넥터 번호로 변경
                    copiedJson.put("connectorId", i);

                    rootJson.put(String.valueOf(i), copiedJson);
                }
            } else {
                rootJson.put(String.valueOf(connectorId), newJson);
            }

            fileManagement.stringToFileSave(
                    GlobalVariables.getRootPath(),
                    "rechgrsocschedule",
                    rootJson.toString(),
                    false);
        } catch (Exception e) {
            logger.error("saveRechgrsocscheduleToFile error : {}", e.getMessage(), e);
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

            RechgrsocscheduleConfirm rechgrsocscheduleConfirm = new RechgrsocscheduleConfirm();
            rechgrsocscheduleConfirm.setStatus(DataTransferStatus.Accepted);

            activity.getSocketReceiveMessage().onResultSend(
                    connectorId,
                    rechgrsocscheduleConfirm.getActionName(),
                    messageId,
                    rechgrsocscheduleConfirm);
        } catch (Exception e) {
            logger.error("sendResponse error : {}", e.getMessage(), e);
        }
    }
}
