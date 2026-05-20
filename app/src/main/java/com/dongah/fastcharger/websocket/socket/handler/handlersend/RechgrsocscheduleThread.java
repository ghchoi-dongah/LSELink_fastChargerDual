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
import com.dongah.fastcharger.sqlite.SQLiteHelper;
import com.dongah.fastcharger.sqlite.dto.CpRechgSoc;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.Objects;

public class RechgrsocscheduleThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(RechgrsocscheduleThread.class);

    private volatile boolean stopped = false;

    public void stopThread() {
        stopped = true;
        interrupt();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        logger.info("RechgrsocscheduleThread start");
        processRechgSoc(0);
        while (!stopped && !isInterrupted()) {
            try {
                Thread.sleep(1000);

                ZonedDateTimeConvert convert = new ZonedDateTimeConvert();
                ZonedDateTime now = convert.doGetCurrentTime();

                int minute = now.getMinute();
                int second = now.getSecond();

                // 정각일 때 실행
                if (minute == 0 && second == 0) {
                    processRechgSoc(0);
                }
            } catch (InterruptedException e) {
                // interrupt()로 인해 발생한 예외이므로 종료를 위해 루프를 빠져나가도록 유도
                logger.info("RechgrsocscheduleThread is interrupted. Stopping...");
                Thread.currentThread().interrupt(); // Interrupt 플래그를 다시 세팅
                break;
            } catch (Exception e) {
                logger.info("RechgrsocscheduleThread error : {}", e.getMessage(), e);
            }
        }
        logger.info("RechgrsocscheduleThread terminated");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void processRechgSoc(int connectorId) {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        SQLiteHelper helper = null;
        SQLiteDatabase sqLiteDatabase;

        try {
            helper = SQLiteHelper.getInstance(activity);
            sqLiteDatabase = helper.getWritableDatabase();
            CpRechgSoc dto = new CpRechgSoc();
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
             * 1. CP_RECHG_SOC 테이블 존재 여부 확인
             *    테이블이 없으면 CP_CHANGE_MODE 테이블 확인
             */
//            helper.dropTable(sqLiteDatabase, tableName);    // drop table
            if (!helper.isTableExists(helper, tableName)) {
                logger.warn("processRechgSoc table not exists : {}", tableName);
                for (int i = startIndex; i <= endIndex; i++) {
                    ChangeModeThread.setChgModeSoc(i);
                }
                return;
            }

            // DH or WH
            ZonedDateTime now = new ZonedDateTimeConvert().doGetCurrentTime();
            if (now == null) return;

            DayOfWeek dayOfWeek = now.getDayOfWeek();

            // DH: 평일, WH: 주말
            boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;

            int hour = now.getHour();
            @SuppressLint("DefaultLocale") String hourKey = String.format("%s%02d", isWeekend ? "WH" : "DH", hour);

            for (int i = startIndex; i <= endIndex; i++) {
                Cursor cursor = null;
                try {
                    cursor = helper.select(tableName,"CONNECTOR_ID=?", new String[]{String.valueOf(i)});
                    // Cursor null 여부 확인, 조회 결과 존재 여부 확인
                    if (cursor == null || !cursor.moveToFirst()) {
                        logger.warn("processRechgSoc {} cursor is null or no data. connectorId : {}", tableName, i);
                        ChangeModeThread.setChgModeSoc(i);
                        continue;
                    }

                    int value = cursor.getInt(cursor.getColumnIndexOrThrow(hourKey));
                    System.out.println("processRechgSoc " + hourKey + " : " + value);

                    ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(i-1);
                    chargingCurrentData.setLimitSoc(value);

                    logger.info("processRechgElec connectorId[{}] limitSoc : {}", i, chargingCurrentData.getLimitSoc());

                    if (Objects.equals(activity.getClassUiProcess(i-1).getUiSeq(), UiSeq.INIT)) {
                        activity.getClassUiProcess(i-1).onHome();
                    }

                    cursor.close();
                } catch (Exception e) {
                    logger.error("processRechgSoc select error : {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("processRechgSoc error : {}", e.getMessage(), e);
        }
    }

    // insert : config soc
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void insertRechgSoc(SQLiteHelper sqLiteHelper, int connectorId) {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            ChargingCurrentData chargingCurrentData = activity.getChargingCurrentData(connectorId-1);
            ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
            ClassUiProcess classUiProcess = activity.getClassUiProcess(connectorId-1);

            CpRechgSoc cpRechgSoc = new CpRechgSoc();
            cpRechgSoc.connectorId = connectorId;

            for (int time = 0; time < 24; time++) {
                cpRechgSoc.dhXX[time] = chargerConfiguration.getTargetSoc();
                cpRechgSoc.whXX[time] = chargerConfiguration.getTargetSoc();
            }

            ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();
            cpRechgSoc.regDt = zonedDateTimeConvert.doGetKstDatetimeAsString();
            cpRechgSoc.modDt = zonedDateTimeConvert.doGetKstDatetimeAsString();

            // insert
            sqLiteHelper.insert(cpRechgSoc);

            chargingCurrentData.setLimitSoc(chargerConfiguration.getTargetSoc());

            if (Objects.equals(classUiProcess.getUiSeq(), UiSeq.INIT)) {
                classUiProcess.onHome();
            }
        } catch (Exception e) {
            logger.error("insertRechgSoc error : {}", e.getMessage(), e);
        }
    }
}
