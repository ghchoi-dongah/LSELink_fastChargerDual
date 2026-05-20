package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.ClassUiProcess;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.controlboard.RxData;
import com.dongah.fastcharger.controlboard.TxData;
import com.dongah.fastcharger.sqlite.SQLiteHelper;
import com.dongah.fastcharger.sqlite.dto.CpChangeMode;
import com.dongah.fastcharger.websocket.ocpp.core.ChargePointStatus;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Objects;

public class ChangeModeThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ChangeModeThread.class);

    private volatile boolean stopped = false;


    public void stopThread() {
        stopped = true;
        interrupt();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        logger.info("ChangeModeThread start");
//        processChgMode(0); // 충전기 부팅 후 1회 실행
        while (!stopped && !isInterrupted()) {
            try {
                Thread.sleep(1000);

                ZonedDateTimeConvert convert = new ZonedDateTimeConvert();
                ZonedDateTime now = convert.doGetCurrentTime();

                int minute = now.getMinute();
                int second = now.getSecond();

                // 정각일 때 충전 모드 변경
                if (minute == 0 && second == 0) {
                    processChgMode(0);
                }
            }  catch (InterruptedException e) {
                // interrupt()로 인해 발생한 예외이므로 종료를 위해 루프를 빠져나가도록 유도
                logger.info("ChangeModeThread is interrupted. Stopping...");
                Thread.currentThread().interrupt(); // Interrupt 플래그를 다시 세팅
                break;
            } catch (Exception e) {
                logger.error("ChangeModeThread error : {}", e.getMessage());
            }
        }
        logger.info("ChangeModeThread terminated");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void processChgMode(int connectorId) {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        SQLiteHelper helper = null;
        SQLiteDatabase sqLiteDatabase;

        try {
            helper = SQLiteHelper.getInstance(activity);
            sqLiteDatabase = helper.getWritableDatabase();
            CpChangeMode dto = new CpChangeMode();
            String tableName = dto.getTableName();

            int startIndex, endIndex;

            /*
             * connectorId == 0 이면 전체 커넥터 조회
             * connectorId != 0 이면 해당 커넥터만 조회
             */
            if (connectorId == 0) {
                startIndex = 1;
                endIndex = GlobalVariables.maxChannel;
            } else {
                startIndex = connectorId;
                endIndex = connectorId;
            }

            /*
             * 1. CP_CHG_MODE 테이블 존재 여부 확인
             *    테이블이 없으면 생성 및 기본값 세팅
             */
            if (!helper.isTableExists(helper, tableName)) {
                logger.warn("processChgMode table not exists : {}", tableName);
                helper.onCreateTable(sqLiteDatabase, tableName);
                for (int i = startIndex; i <= endIndex; i++) {
                    insertChgMode(helper, i);
                    updateChgModeStatus(i, "DM");
                }
                return;
            }

            /*
             * 현재 시간 확인
             * 시간별 컬럼명 HH00 ~ HH23 생성
             */
            ZonedDateTime now = new ZonedDateTimeConvert().doGetCurrentTime();
            if (now == null) return;

            int hour = now.getHour();
            @SuppressLint("DefaultLocale") String hourKey = String.format("HH%02d", hour);

            for (int i = startIndex; i <= endIndex; i++) {
                Cursor cursor = null;
                try {
                    cursor = helper.select(tableName,"CONNECTOR_ID=?", new String[]{String.valueOf(i)});
                    // Cursor null 여부 확인, 조회 결과 존재 여부 확인
                    if (cursor == null || !cursor.moveToFirst()) {
                        logger.warn("processChgMode {} cursor is null or no data. connectorId : {}", tableName, i);
                        insertChgMode(helper, i);
                        updateChgModeStatus(i, "DM");
                        continue;
                    }

                    String value = cursor.getString(cursor.getColumnIndexOrThrow(hourKey));
                    System.out.println("processChgMode " + hourKey + " : " + value);

                    updateChgModeStatus(i, value);
                    cursor.close();
                } catch (Exception e) {
                    logger.error("processChgMode select error : {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("processChgMode error : {}", e.getMessage(), e);
        }
    }

    // insert : DM
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void insertChgMode(SQLiteHelper sqLiteHelper, int connectorId) {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
            ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(connectorId-1);

            CpChangeMode cpChangeMode = new CpChangeMode();
            cpChangeMode.connectorId = connectorId;
            cpChangeMode.rechgAmt = chargerConfiguration.getTargetSoc();
            cpChangeMode.rechgElec = chargerConfiguration.getDr();

            for (int time = 0; time < 24; time++) {
                cpChangeMode.hhXX[time] = "DM";
            }

            ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();
            cpChangeMode.regDt = zonedDateTimeConvert.doGetKstDatetimeAsString();
            cpChangeMode.modDt = zonedDateTimeConvert.doGetKstDatetimeAsString();

            // insert
            sqLiteHelper.insert(cpChangeMode);

            chargingCurrentData.setChangeMode("DM");
            chargingCurrentData.setConnectUse(true);

//            updateChgModeStatus(connectorId, "DM");
        } catch (Exception e) {
            logger.error("insertChgMode error : {}", e.getMessage(), e);
        }
    }

    // update change mode status
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void updateChgModeStatus(int connectorId, String status) {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();

        try {
            ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(connectorId-1);
            if (Objects.equals(chargingCurrentData.getChangeMode(), status)) return;
            chargingCurrentData.setChangeMode(status);

            ChargePointStatus currentStatus = chargingCurrentData.getChargePointStatus();
            StatusNotificationReq statusNotificationReq = new StatusNotificationReq(connectorId);

            RxData rxData = activity.getControlBoard().getRxData(connectorId-1);
            int priority = chargerConfiguration.getConnectorPriority();
            ChargePointStatus targetStatus;

            // 1구 충전 우선순위
            // priority == 1 : ch0 사용 / ch1 미사용
            // priority == 2 : ch0 미사용 / ch1 사용
            if (status.equals("DM")) {
                targetStatus = ChargePointStatus.Available;
                if (Objects.equals(currentStatus, ChargePointStatus.Unavailable)) {
                    chargingCurrentData.setChargePointStatus(ChargePointStatus.Available);
                    statusNotificationReq.sendStatusNotification(connectorId, ChargePointStatus.Available);
                }
            } else if (status.equals("NM")) {
                if (priority == 1) {
                    targetStatus = connectorId != 1
                            ? ChargePointStatus.Unavailable
                            : rxData.isCsPilot() ? ChargePointStatus.Preparing : ChargePointStatus.Available;
                } else {
                    targetStatus = connectorId != 2
                            ? ChargePointStatus.Unavailable
                            : rxData.isCsPilot() ? ChargePointStatus.Preparing : ChargePointStatus.Available;
                }

                UiSeq uiSeq = activity.getClassUiProcess(connectorId-1).getUiSeq();

                if (!Objects.equals(currentStatus, targetStatus)) {
                    boolean isCheck = uiSeq.equals(UiSeq.CHARGING) || uiSeq.equals(UiSeq.FINISH_WAIT) || uiSeq.equals(UiSeq.FINISH);
//                    Objects.equals(currentStatus, ChargePointStatus.Available) || Objects.equals(currentStatus, ChargePointStatus.Unavailable) || Objects.equals(currentStatus, ChargePointStatus.Preparing);

                    if (!isCheck) {
                        chargingCurrentData.setChargePointStatus(targetStatus);
                        statusNotificationReq.sendStatusNotification(connectorId, targetStatus);
                    }
                }
            } else {
                targetStatus = ChargePointStatus.Unavailable;
                if (Objects.equals(currentStatus, ChargePointStatus.Available)) {
                    chargingCurrentData.setChargePointStatus(ChargePointStatus.Unavailable);
                    statusNotificationReq.sendStatusNotification(connectorId, ChargePointStatus.Unavailable);
                }
            }

            boolean isModeValid = Objects.equals(targetStatus, ChargePointStatus.Unavailable);
            chargingCurrentData.setConnectUse(!isModeValid);

            if (Objects.equals(activity.getClassUiProcess(connectorId-1).getUiSeq(), UiSeq.INIT)) {
                activity.getClassUiProcess(connectorId-1).onHome();
            }
        } catch (Exception e) {
            logger.error("setChgModeStatus error : {}", e.getMessage(), e);
        }
    }

    // CP_CHANGE_MODE
    // rechgElec 설정
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void setChgModeElec(int connectorId) {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
        SQLiteHelper helper = null;

        try {
            ClassUiProcess classUiProcess = activity.getClassUiProcess(connectorId-1);
            helper = SQLiteHelper.getInstance(activity);
            CpChangeMode dto = new CpChangeMode();
            String tableName = dto.getTableName();

            // Check if the table exists
            TxData txData = activity.getControlBoard().getTxData(connectorId-1);
            if (!helper.isTableExists(helper, tableName)) {
                logger.warn("setChgModeElec {} doesn't exist", tableName);
                txData.setOutPowerLimit((short) chargerConfiguration.getDr());
                if (Objects.equals(classUiProcess.getUiSeq(), UiSeq.INIT)) classUiProcess.onHome();
                return;
            }

            Cursor cursor = helper.select(tableName,"CONNECTOR_ID=?", new String[]{String.valueOf(connectorId)});

            // Cursor null 여부 확인, 조회 결과 존재 여부 확인
            if (cursor == null || !cursor.moveToFirst()) {
                logger.warn("setChgModeElec {} cursor is null or no data. connectorId : {}", tableName, connectorId);
                txData.setOutPowerLimit((short) chargerConfiguration.getDr());
                if (Objects.equals(classUiProcess.getUiSeq(), UiSeq.INIT)) classUiProcess.onHome();
                return;
            }

            int value = cursor.getInt(cursor.getColumnIndexOrThrow("RECHG_ELEC"));
            if (value == 0) {
                txData.setOutPowerLimit((short) chargerConfiguration.getDr());
            } else {
                txData.setOutPowerLimit((short) value);
            }

            logger.info("setChgModeElec connectorId[{}] outPowerLimit : {}", connectorId, txData.getOutPowerLimit());
            if (Objects.equals(classUiProcess.getUiSeq(), UiSeq.INIT)) classUiProcess.onHome();

            cursor.close();
        } catch (Exception e) {
            logger.error("setChgModeElec error : {}", e.getMessage(), e);
        }
    }

    // CP_CHANGE_MODE
    // rechgAmt 변경
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void setChgModeSoc(int connectorId) {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
        SQLiteHelper helper = null;

        try {
            ClassUiProcess classUiProcess = activity.getClassUiProcess(connectorId-1);
            helper = SQLiteHelper.getInstance(activity);
            CpChangeMode dto = new CpChangeMode();
            String tableName = dto.getTableName();

            ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(connectorId-1);
            if (!helper.isTableExists(helper, tableName)) {
                logger.warn("setChgModeSoc {} doesn't exist", tableName);
                chargingCurrentData.setLimitSoc(chargerConfiguration.getTargetSoc());
                if (Objects.equals(classUiProcess.getUiSeq(), UiSeq.INIT)) classUiProcess.onHome();
                return;
            }

            Cursor cursor = helper.select(tableName,"CONNECTOR_ID=?", new String[]{String.valueOf(connectorId)});

            // Cursor null 여부 확인, 조회 결과 존재 여부 확인
            if (cursor == null || !cursor.moveToFirst()) {
                logger.warn("setChgModeSoc {} cursor is null or no data. connectorId : {}", tableName, connectorId);
                chargingCurrentData.setLimitSoc(chargerConfiguration.getTargetSoc());
                if (Objects.equals(classUiProcess.getUiSeq(), UiSeq.INIT)) classUiProcess.onHome();
                return;
            }

            int value = cursor.getInt(cursor.getColumnIndexOrThrow("RECHG_AMT"));
            if (value == 0) {
                chargingCurrentData.setLimitSoc(chargerConfiguration.getTargetSoc());
            } else {
                chargingCurrentData.setLimitSoc(value);
            }

            logger.info("setChgModeSoc connectorId[{}] limitSOc : {}", connectorId, chargingCurrentData.getLimitSoc());
            if (Objects.equals(classUiProcess.getUiSeq(), UiSeq.INIT)) classUiProcess.onHome();

            cursor.close();
        } catch (Exception e) {
            logger.error("setChgModeSoc error : {}", e.getMessage(), e);
        }
    }
}
