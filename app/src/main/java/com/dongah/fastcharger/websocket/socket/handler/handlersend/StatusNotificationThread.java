package com.dongah.fastcharger.websocket.socket.handler.handlersend;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatusNotificationThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(StatusNotificationThread.class);

    private volatile boolean stopped = false;
    private final int delayTime;
    private int count = 0;

    StatusNotificationReq statusNotificationReq;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public StatusNotificationThread(int delayTime) {
        this.delayTime = delayTime;
        statusNotificationReq = new StatusNotificationReq(0);
        statusNotificationReq.sendStatusNotification();
    }

    public void stopThread() {
        stopped = true;
        interrupt();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void run() {
        logger.info("StatusNotificationThread start");
        while (!stopped && !isInterrupted()) {
            try {
                Thread.sleep(1000);
                count++;
                if (count >= delayTime) {
                    count = 0;
                    statusNotificationReq.sendStatusNotification();
                }
            } catch (InterruptedException e) {
                // interrupt()로 인해 발생한 예외이므로 종료를 위해 루프를 빠져나가도록 유도
                logger.info("StatusNotificationThread is interrupted. Stopping...");
                Thread.currentThread().interrupt(); // Interrupt 플래그를 다시 세팅
                break;
            } catch (Exception e) {
                logger.error("StatusNotificationThread error : {}", e.getMessage());
            }
        }
        logger.info("StatusNotificationThread terminated");
    }
}
