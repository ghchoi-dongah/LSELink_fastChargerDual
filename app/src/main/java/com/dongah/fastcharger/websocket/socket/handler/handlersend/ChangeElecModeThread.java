package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import androidx.annotation.RequiresApi;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ClassUiProcess;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.controlboard.TxData;
import com.dongah.fastcharger.sqlite.SQLiteHelper;
import com.dongah.fastcharger.sqlite.dto.CpChgElecmode;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Objects;

public class ChangeElecModeThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ChangeElecModeThread.class);

    private volatile boolean stopped = false;

    public void stopThread() {
        stopped = true;
        interrupt();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        logger.info("ChangeElecModeThread start");

        processRechgElec(0);
        while (!stopped && !isInterrupted()) {
            try {
                Thread.sleep(1000);

                ZonedDateTimeConvert convert = new ZonedDateTimeConvert();
                ZonedDateTime now = convert.doGetCurrentTime();

                int minute = now.getMinute();
                int second = now.getSecond();

                // 정각일 때 충전 모드 변경
                if (minute == 0 && second == 0) {
                    processRechgElec(0);
                }
            }  catch (InterruptedException e) {
                // interrupt()로 인해 발생한 예외이므로 종료를 위해 루프를 빠져나가도록 유도
                logger.info("ChangeElecModeThread is interrupted. Stopping...");
                Thread.currentThread().interrupt(); // Interrupt 플래그를 다시 세팅
                break;
            } catch (Exception e) {
                logger.error("ChangeElecModeThread error : {}", e.getMessage(), e);
            }
        }
        logger.info("ChangeElecModeThread terminated");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void processRechgElec(int connectorId) {
        MainActivity activity = (MainActivity) MainActivity.mContext;
        ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
        SQLiteHelper helper = null;
        SQLiteDatabase sqLiteDatabase;

        try {
            helper = SQLiteHelper.getInstance(activity);
            sqLiteDatabase = helper.getWritableDatabase();
            CpChgElecmode dto = new CpChgElecmode();
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
             * 1. CP_CHG_ELECMODE 테이블 존재 여부 확인
             *    테이블이 없으면 CP_CHANGE_MODE 테이블 확인
             */
            if (!helper.isTableExists(helper, tableName)) {
                logger.warn("processRechgElec table not exists : {}", tableName);
                for (int i = startIndex; i <= endIndex; i++) {
                    ChangeModeThread.setChgModeElec(i);
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
                        logger.warn("processRechgElec {} cursor is null or no data. connectorId : {}", tableName, i);
                        ChangeModeThread.setChgModeElec(i);
                        continue;
                    }

                    int value = cursor.getInt(cursor.getColumnIndexOrThrow(hourKey));
                    System.out.println("processRechgElec " + hourKey + " : " + value);

                    // 시간대 전력 설정
                    TxData txData = activity.getControlBoard().getTxData(i-1);
                    if (value == 0) {
                        txData.setOutPowerLimit((short) chargerConfiguration.getDr());
                    } else {
                        txData.setOutPowerLimit((short) value);
                    }

                    logger.info("processRechgElec connectorId[{}] outPowerLimit : {}", i, txData.getOutPowerLimit());

                    if (Objects.equals(activity.getClassUiProcess(i-1).getUiSeq(), UiSeq.INIT)) {
                        activity.getClassUiProcess(i-1).onHome();
                    }

                    cursor.close();
                } catch (Exception e) {
                    logger.error("processRechgElec select error : {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("processRechgElec error : {}", e.getMessage(), e);
        }
    }

    // insert : config 전력
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void insertChgElecMode(SQLiteHelper sqLiteHelper, int connectorId) {
        try {
            MainActivity activity = (MainActivity) MainActivity.mContext;
            ChargerConfiguration chargerConfiguration = activity.getChargerConfiguration();
            ClassUiProcess classUiProcess = activity.getClassUiProcess(connectorId-1);

            // config dr setting
            CpChgElecmode cpChgElecmode = new CpChgElecmode();
            cpChgElecmode.connectorId = connectorId;

            for (int time = 0; time < 24; time++) {
                cpChgElecmode.hhXX[time] = chargerConfiguration.getDr();
            }

            ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();
            cpChgElecmode.regDt = zonedDateTimeConvert.doGetKstDatetimeAsString();
            cpChgElecmode.modDt = zonedDateTimeConvert.doGetKstDatetimeAsString();

            // insert
            sqLiteHelper.insert(cpChgElecmode);

            TxData txData = activity.getControlBoard().getTxData(connectorId-1);
            txData.setOutPowerLimit((short) chargerConfiguration.getDr());
            logger.info("insertChgElecMode connectorId[{}] outPowerLimit : {}", connectorId, txData.getOutPowerLimit());

            if (Objects.equals(classUiProcess.getUiSeq(), UiSeq.INIT)) {
                classUiProcess.onHome();
            }
        } catch (Exception e) {
            logger.error("insertChgElecMode error : {}", e.getMessage(), e);
        }
    }
}
