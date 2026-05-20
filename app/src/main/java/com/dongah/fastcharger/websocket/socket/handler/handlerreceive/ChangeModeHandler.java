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
import com.dongah.fastcharger.sqlite.dto.CpChangeMode;
import com.dongah.fastcharger.sqlite.dto.CpChgElecmode;
import com.dongah.fastcharger.sqlite.dto.CpRechgSoc;
import com.dongah.fastcharger.utils.FileManagement;
import com.dongah.fastcharger.websocket.ocpp.core.DataTransferStatus;
import com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink.ChangeModeConfirm;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;
import com.dongah.fastcharger.websocket.socket.OcppHandler;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.ChangeElecModeThread;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.ChangeModeThread;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.RechgrsocscheduleThread;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;

public class ChangeModeHandler implements OcppHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChangeModeHandler.class);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void handle(JSONObject payload, int connectorId, String messageId) throws Exception {
        try {
            String vendorId = payload.getString("vendorId");
            String msgId = payload.getString("messageId");  //changemode.req
            String dataStr = payload.getString("data");

            // file save
            saveChangeModeToFile(dataStr);
            // response
            sendResponse(connectorId, messageId);

            /* DB update
            * CP_CHANGE_MODE update
            * CP_CHG_ELECMODE RECHG_ELEC update
            * CP_RECHG_SOC  update
            * */
            if (connectorId == 0) {
                for (int i = 1; i <= GlobalVariables.maxChannel; i++) {
                    updateChgMode(dataStr, i);
                    updateChgElecMode(i);
                    updateRechgSoc(i);
                }
            } else {
                updateChgMode(dataStr, connectorId);
                updateChgElecMode(connectorId);
                updateRechgSoc(connectorId);
            }

            // 커넥터 모드 변경(DM, IM, WM, NM)
            ChangeModeThread.processChgMode(connectorId);

            // 전력, SoC 변경
            ChangeElecModeThread.processRechgElec(connectorId);
            RechgrsocscheduleThread.processRechgSoc(connectorId);

        } catch (Exception e) {
            logger.error("ChangeModeHandler error : {}", e.getMessage(), e);
        }
    }

    /**
     * data JSON을 파싱하여 CP_CHANGE_MODE 테이블을 업데이트한다.
     * - connectorId == 0 : CONNECTOR_ID 1, 2 모두 업데이트
     * - connectorId == 1 또는 2 : 해당 CONNECTOR_ID만 업데이트
     */
    @SuppressLint("DefaultLocale")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateChgMode(String dataStr, int connectorId) {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            SQLiteHelper helper = SQLiteHelper.getInstance(activity);
            SQLiteDatabase sqLiteDatabase = helper.getWritableDatabase();
            String tableName = new CpChangeMode().getTableName();

            JSONObject dataJson = new JSONObject(dataStr);

            // 테이블 없으면 테이블 생성 후 업데이트 진행
            if (!helper.isTableExists(helper, tableName)) {
                logger.warn("updateChgMode table not exists : {}", tableName);
                helper.onCreateTable(sqLiteDatabase, tableName);
                ChangeModeThread.insertChgMode(helper, connectorId);    // connectorId 1, 2 기본 세팅
                ChangeModeThread.updateChgModeStatus(connectorId, "DM");
            }

            // connector_id 존재 유무 확인
            Cursor cursor = helper.select(tableName,"CONNECTOR_ID=?", new String[]{String.valueOf(connectorId)});
            // Cursor null 여부 확인, 조회 결과 존재 여부 확인
            if (cursor == null || !cursor.moveToFirst()) {
                logger.warn("updateChgMode {} cursor is null or no data. connectorId : {}", tableName, connectorId);
                insertChgMode(helper, connectorId, dataJson);
                return;
            }

            // rechgAmt, rechgElec 값 추출
            ContentValues values = new ContentValues();
            if (dataJson.has("rechgAmt")) {
                values.put("RECHG_AMT", dataJson.getInt("rechgAmt"));
            }
            if (dataJson.has("rechgElec")) {
                values.put("RECHG_ELEC", dataJson.getInt("rechgElec"));
            }

            // HH00 ~ HH23 값 추출
            for (int i = 0; i < 24; i++) {
                String key = String.format("HH%02d", i);
                if (dataJson.has(key)) {
                    values.put(key, dataJson.getString(key));
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
                    logger.info("updateChgMode connectorId[{}] updated rows : {}", id, updated);
                }
            } else {
                // connectorId가 1 또는 2이면 해당 CONNECTOR_ID만 업데이트
                int updated = helper.update(tableName, values, "CONNECTOR_ID=?", new String[]{String.valueOf(connectorId)});
                logger.info("updateChgMode single connectorId[{}] updated rows : {}", connectorId, updated);
            }

            cursor.close();
        } catch (Exception e) {
            logger.error("updateChgMode error : {}", e.getMessage(), e);
        }
    }

    // CP_CHANGE_MODE insert
    @SuppressLint("DefaultLocale")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void insertChgMode(SQLiteHelper sqLiteHelper, int connectorId, JSONObject dataJson) {
        try {
            CpChangeMode cpChangeMode = new CpChangeMode();
            cpChangeMode.connectorId = connectorId;
            cpChangeMode.rechgAmt = dataJson.getInt("rechgAmt");
            cpChangeMode.rechgElec = dataJson.getInt("rechgElec");

            for (int time = 0; time < 24; time++) {
                String key = String.format("HH%02d", time);
                if (dataJson.has(key)) {
                    cpChangeMode.hhXX[time] = dataJson.getString(key);
                }
            }

            ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();
            cpChangeMode.regDt = zonedDateTimeConvert.doGetKstDatetimeAsString();
            cpChangeMode.modDt = zonedDateTimeConvert.doGetKstDatetimeAsString();

            sqLiteHelper.insert(cpChangeMode);
        } catch (Exception e) {
            logger.error("insertChgMode error : {}", e.getMessage(), e);
        }
    }


    /*
     * changeMode, changeElecMode 중 더 작은 전력 설정
     * */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateChgElecMode(int connectorId) {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        SQLiteHelper helper = null;

        try {
            helper = SQLiteHelper.getInstance(activity);
            CpChgElecmode cpChgElecmode = new CpChgElecmode();
            String tableNameElec = cpChgElecmode.getTableName();

            // CP_CHGELEC_MODE 테이블 존재 유무 확인
            if (!helper.isTableExists(helper, tableNameElec)) return;

            CpChangeMode cpChangeMode = new CpChangeMode();
            String tableNameMode = cpChangeMode.getTableName();

            // 현재 시간 확인
            ZonedDateTime now = new ZonedDateTimeConvert().doGetCurrentTime();
            if (now == null) return;

            int hour = now.getHour();
            @SuppressLint("DefaultLocale") String hourKey = String.format("HH%02d", hour);

            // CP_CHG_ELECMODEE 현재시간 전력, CP_CHANGE_MODE RECHG_ELEC 추출
            Cursor cursorElec = helper.select(tableNameElec,"CONNECTOR_ID=?", new String[]{String.valueOf(connectorId)});
            Cursor cursorMode = helper.select(tableNameMode,"CONNECTOR_ID=?", new String[]{String.valueOf(connectorId)});

            // CP_CHGELEC_MODE Cursor null 여부 확인, 조회 결과 존재 여부 확인
            if (cursorElec == null || !cursorElec.moveToFirst()) {
                logger.warn("updateChgElecMode CP_CHGELEC_MODE cursor is null or no data. connectorId : {}", connectorId);
                return;
            }

            // CP_CHANGE_MODE Cursor null 여부 확인, 조회 결과 존재 여부 확인
            if (cursorMode == null || !cursorMode.moveToFirst()) {
                logger.warn("updateChgElecMode CP_CHANGE_MODE is null or no data. connectorId : {}", connectorId);
                return;
            }

            int currPower = cursorElec.getInt(cursorElec.getColumnIndexOrThrow(hourKey));
            int newPower = cursorMode.getInt(cursorMode.getColumnIndexOrThrow("RECHG_ELEC"));
            logger.info("updateChgElecMode connectorId[{}] currPower : {}, newPower : {}", connectorId, currPower, newPower);

            // update
            if (currPower > newPower) {
                ContentValues values = new ContentValues();
                // 현재 시간 전력량 업데이트
                values.put(hourKey, newPower);

                // MOD_DT 업데이트
                ZonedDateTimeConvert convert = new ZonedDateTimeConvert();
                values.put("MOD_DT", convert.doGetKstDatetimeAsString());

                int updated = helper.update(tableNameElec, values, "CONNECTOR_ID=?", new String[]{String.valueOf(connectorId)});
                logger.info("updateChgElecMode single connectorId[{}] updated rows : {}", connectorId, updated);
            }

            cursorElec.close();
            cursorMode.close();
        } catch (Exception e) {
            logger.error("updateChgElecMode error : {}", e.getMessage(), e);
        }
    }

    /*
     * changeMode, rechgrsocschedule 중 더 작은 soc 설정
     * */
    @SuppressLint("DefaultLocale")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateRechgSoc(int connectorId) {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        SQLiteHelper helper = null;

        try {
            helper = SQLiteHelper.getInstance(activity);
            CpRechgSoc cpRechgSoc = new CpRechgSoc();
            String tableNameSoc = cpRechgSoc.getTableName();

            // CP_RECHG_SOC 테이블 존재 유무 확인
            if (!helper.isTableExists(helper, tableNameSoc)) return;

            CpChangeMode cpChangeMode = new CpChangeMode();
            String tableNameMode = cpChangeMode.getTableName();

            // 평일 or 주말 확인
            ZonedDateTime now = new ZonedDateTimeConvert().doGetCurrentTime();
            if (now == null) return;

            DayOfWeek dayOfWeek = now.getDayOfWeek();

            // DH: 평일, WH: 주말
            boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;

            int hour = now.getHour();
            String hourKey = String.format("%s%02d", isWeekend ? "WH" : "DH", hour);

            // CP_RECHG_SOC 현재시간 SoC, CP_CHANGE_MODE RECHG_AMT 추출
            Cursor cursorSoc = helper.select(tableNameSoc,"CONNECTOR_ID=?", new String[]{String.valueOf(connectorId)});
            Cursor cursorMode = helper.select(tableNameMode,"CONNECTOR_ID=?", new String[]{String.valueOf(connectorId)});

            // CP_RECHG_SOC Cursor null 여부 확인, 조회 결과 존재 여부 확인
            if (cursorSoc == null || !cursorSoc.moveToFirst()) {
                logger.warn("updateRechgSoc CP_RECHG_SOC cursor is null or no data. connectorId : {}", connectorId);
                return;
            }

            // CP_CHANGE_MODE Cursor null 여부 확인, 조회 결과 존재 여부 확인
            if (cursorMode == null || !cursorMode.moveToFirst()) {
                logger.warn("updateRechgSoc CP_CHANGE_MODE is null or no data. connectorId : {}", connectorId);
                return;
            }

            int currSoc = cursorSoc.getInt(cursorSoc.getColumnIndexOrThrow(hourKey));
            int newSoc = cursorMode.getInt(cursorMode.getColumnIndexOrThrow("RECHG_AMT"));
            logger.info("updateRechgSoc connectorId[{}] currSoc : {}, newSoc : {}", connectorId, currSoc, newSoc);

            // update
            if (currSoc > newSoc) {
                ContentValues values = new ContentValues();
                values.put(hourKey, newSoc);

                // MOD_DT 업데이트
                ZonedDateTimeConvert convert = new ZonedDateTimeConvert();
                values.put("MOD_DT", convert.doGetKstDatetimeAsString());

                int updated = helper.update(tableNameSoc, values, "CONNECTOR_ID=?", new String[]{String.valueOf(connectorId)});
                logger.info("updateRechgSoc single connectorId[{}] updated rows : {}", connectorId, updated);
            }

            cursorSoc.close();
            cursorMode.close();
        } catch (Exception e) {
            logger.error("updateRechgSoc error : {}", e.getMessage(), e);
        }
    }

    private void saveChangeModeToFile(String newData) {
        try {
            FileManagement fileManagement = new FileManagement();
            JSONObject rootJson;

            File file = new File(GlobalVariables.getRootPath() + File.separator + GlobalVariables.FILE_CHANGE_MODE);


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
                // connectorId가 1, 2 등 특정 커넥터이면 해당 커넥터만 저장
                rootJson.put(String.valueOf(connectorId), newJson);
            }

            fileManagement.stringToFileSave(
                    GlobalVariables.getRootPath(),
                    "changeMode",
                    rootJson.toString(),
                    false);
        } catch (Exception e) {
            logger.error("saveChangeModeToFile error : {}", e.getMessage(), e);
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

            ChangeModeConfirm changeModeConfirm = new ChangeModeConfirm();
            changeModeConfirm.setStatus(DataTransferStatus.Accepted);

            activity.getSocketReceiveMessage().onResultSend(
                    connectorId,
                    changeModeConfirm.getActionName(),
                    messageId,
                    changeModeConfirm);
        } catch (Exception e) {
            logger.error(" sendResponse error : {}", e.getMessage());
        }
    }
}
