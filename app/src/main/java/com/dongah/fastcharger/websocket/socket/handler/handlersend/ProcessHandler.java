package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessHandler extends Handler {
    private static final Logger logger = LoggerFactory.getLogger(ProcessHandler.class);

    Bundle bundle;
    HeartbeatThread heartbeatThread;
    BootNotificationThread bootNotificationThread;
    StatusNotificationThread statusNotificationThread;
    ChangeModeThread changeModeThread;
    ChangeElecModeThread changeElecModeThread;
    RechgrsocscheduleThread rechgrsocscheduleThread;
    DiagnosticsThread diagnosticsThread;


    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        bundle = msg.getData();
    }


    /**
     * Heart beat thread
     *
     * @param delay delay time
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onHeartBeatStart(int delay) {
        onHeartBeatStop();
        if (heartbeatThread == null || heartbeatThread.getState() != Thread.State.RUNNABLE) {
            heartbeatThread = new HeartbeatThread(delay);
            heartbeatThread.start();
        }
    }

    public void onHeartBeatStop() {
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
            heartbeatThread.stopThread();
            heartbeatThread = null;
        }
    }

    /**
     * boot notification
     *
     * @param delay delay time
     */
    public void onBootNotificationStart(int delay) {
        onBootNotificationStop();
        bootNotificationThread = new BootNotificationThread(delay);
        bootNotificationThread.start();
    }

    public void onBootNotificationStop() {
        if (bootNotificationThread != null) {
            bootNotificationThread.interrupt();
            bootNotificationThread.stopThread();
            bootNotificationThread = null;
        }
    }

    //StatusNotificationThread

    /**
     * Status Notification
     * @param delay 5분
     */

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void onStatusNotificationStart(int delay) {
        onStatusNotificationStop();
        statusNotificationThread = new StatusNotificationThread(delay);
        statusNotificationThread.start();
    }

    public void onStatusNotificationStop() {
        if (statusNotificationThread != null) {
            statusNotificationThread.interrupt();
            statusNotificationThread.stopThread();
            statusNotificationThread = null;
        }
    }


    /**
     * DT(changeMode) Thread
     */

    public void onChangeModeStart() {
        onChangeModeStop();
        changeModeThread = new ChangeModeThread();
        changeModeThread.start();
    }

    public void onChangeModeStop() {
        if (changeModeThread != null) {
            changeModeThread.interrupt();
            changeModeThread.stopThread();
            changeModeThread = null;
        }
    }

    /**
     * DT(changeelecmode) Thread
     */
    public void onChangeElecModeStart() {
        onChangeElecModeStop();
        changeElecModeThread = new ChangeElecModeThread();
        changeElecModeThread.start();
    }

    public void onChangeElecModeStop() {
        if (changeElecModeThread != null) {
            changeElecModeThread.interrupt();
            changeElecModeThread.stopThread();
            changeElecModeThread = null;
        }
    }

    /**
     * DT(rechgrsocschedule) Thread
     * */
    public void onRechgrsocscheduleStart() {
        onRechgrsocscheduleStop();
        rechgrsocscheduleThread = new RechgrsocscheduleThread();
        rechgrsocscheduleThread.start();
    }

    public void onRechgrsocscheduleStop() {
        if (rechgrsocscheduleThread != null) {
            rechgrsocscheduleThread.interrupt();
            rechgrsocscheduleThread.stopThread();
            rechgrsocscheduleThread = null;
        }
    }

    /**
     * DiagnosticsThread
     * @param delayTime delay time
     * */
    public void onDiagnosticsStart(long delayTime) {
        onDiagnosticsStop();
        diagnosticsThread = new DiagnosticsThread(delayTime);
        diagnosticsThread.start();
    }

    public void onDiagnosticsStop() {
        if (diagnosticsThread != null) {
            diagnosticsThread.interrupt();
            diagnosticsThread.stopThread();
            diagnosticsThread = null;
        }
    }
}
