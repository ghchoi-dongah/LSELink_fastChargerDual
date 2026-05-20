package com.dongah.fastcharger.utils;

import android.annotation.SuppressLint;

import com.dongah.fastcharger.basefunction.GlobalVariables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class LogDataSave {

    public static final Logger logger = LoggerFactory.getLogger(LogDataSave.class);

    FileManagement fileManagement;
    String rootPath;
    String logType;

    String[] actionNames = {"StartTransaction", "StopTransaction", "getPrice", "partialCancel", "payInfo",
            "resultPrice", "MeterValues", "BootNotification", "RemoteStartTransaction", "StatusNotification",
            "RemoteStopTransaction", "Reset", "StatusNotification", "Authorize", "announceMessage", "smsMessage"};
    ArrayList<String> actionList = new ArrayList<>();

    public LogDataSave() {
        fileManagement = new FileManagement();
    }

    public LogDataSave(String logType) {
        this.logType = logType;
        Collections.addAll(actionList, actionNames);
        fileManagement = new FileManagement();
        File parent = new File(GlobalVariables.ROOT_PATH + File.separator + logType);
        if (!parent.exists()) {
            boolean aCheck = parent.mkdir();
        }

        // log를 connectorId별 구분하기 위한 폴더 생성
        if (logType.equals("log")) {
            for (int i = 0; i <= GlobalVariables.maxChannel; i++) {
                createConnectorDir(i);
            }
        }

        setRootPath(GlobalVariables.ROOT_PATH + File.separator + logType);
    }

    private void createConnectorDir(int connectorId) {
        String path = GlobalVariables.ROOT_PATH + File.separator + logType + File.separator + connectorId;
        File dir = new File(path);
        if (!dir.exists()) {
            boolean check = dir.mkdir();
        }
    }

    public void makeLogDate(int connectorId, String actionName, String logData) {
        try {
            String fileName = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
            String path = getConnectorPath(connectorId);
            boolean check;

            File dir = new File(path);
            if (!dir.exists()) {
                check = dir.mkdirs();
            }

            if ("<<send fail>>".equals(actionName)) {
                logData = actionName + " " + logData;
            }

            fileManagement.stringToFileSave(path, fileName, logData, true);
        } catch (Exception e) {
            logger.error("Log data save fail", e);
        }
    }

    public void makeLogDate(String actionName, String logData) {
        try {
            String fileName = (new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()));
            fileManagement.stringToFileSave(getRootPath(), fileName, logData, true);
//            if (actionList.contains(actionName)) {
//            }
        } catch (Exception e) {
            logger.error("Log data save fail : {}", e.getMessage());
        }
    }


    public void makeDump(int connectorId, String data) {
        try {
            boolean chk;
            rootPath = GlobalVariables.getRootPath() + File.separator + "dump";
            String fileName = "dump" + connectorId;
            File parent = new File(rootPath);
            if (!parent.exists()) chk = parent.mkdir();
            fileManagement.stringToFileSave(rootPath, fileName, data, true);
        } catch (Exception e) {
            logger.error("dump data save fail : {} ", e.getMessage());
        }
    }

    /**
     * 40kW, 50kW 충전 효율 비교
     * 충전시간|충전남은시간|출력전압|출력전류|출력전력
     *
     * @param data : charging history data
     */
    public void chargingHistory(String data) {
        try {
            boolean chk;
            String path = GlobalVariables.getRootPath() + File.separator + "history";
            File parent = new File(path);
            if (!parent.exists()) chk = parent.mkdir();
            String fileName = "history_" + (new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()));
            fileManagement.stringToFileSave(path, fileName, data, true);
        } catch (Exception e) {
            logger.error("charging history error : {}", e.getMessage());
        }
    }

    /**
     * log data delete 30일 초과 데이터
     */
//    public void removeLogData() {
//        String fName;
//        Date fDate, tDate;
//        long calDate;
//        long calDateDays;
//        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateSet = new SimpleDateFormat("yyyyMMdd");
//
//        //30일 지난 화일만 삭제
//        try {
//            boolean chk;
//            tDate = dateSet.parse(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()));
//            File directory = new File(getRootPath());
//            File[] files = directory.listFiles();
//            if (files != null) {
//                for (File file : files) {
//                    fName = file.getName();
//                    fDate = dateSet.parse(fName);
//                    calDate = (tDate != null ? tDate.getTime() : 0) - (fDate != null ? fDate.getTime() : 0);
//                    calDateDays = calDate / (24 * 60 * 60 * 1000);
//                    if (calDateDays > 30) {
//                        chk = file.delete();
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logger.error(e.getMessage());
//        }

    public void removeLogData() {
        String fName;
        Date fDate, tDate;
        long calDate;
        long calDateDays;
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateSet = new SimpleDateFormat("yyyyMMdd");

        try {
            boolean chk;
            tDate = dateSet.parse(new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()));

            // 0/1/2 각각 순회
            for (int connectorId = 0; connectorId <= GlobalVariables.maxChannel; connectorId++) {
                File directory = new File(getConnectorPath(connectorId));
                File[] files = directory.listFiles();
                if (files == null) continue;

                for (File file : files) {
                    fName = file.getName();
                    fDate = dateSet.parse(fName);
                    calDate = (tDate != null ? tDate.getTime() : 0) - (fDate != null ? fDate.getTime() : 0);
                    calDateDays = calDate / (24 * 60 * 60 * 1000);
                    if (calDateDays > 30) {
                        chk = file.delete();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("removeLogData error", e);
        }
    }

    private int normalizeConnectorId(int connectorId) {
        return (connectorId == 1 || connectorId == 2) ? connectorId : 0;
    }

    private String getConnectorPath(int connectorId) {
        int normalized = normalizeConnectorId(connectorId);
        return getRootPath() + File.separator + normalized;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }
}