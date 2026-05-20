package com.dongah.fastcharger.rfcard;

import com.dongah.fastcharger.utils.BitUtilities;
import com.dongah.fastcharger.utils.CRC16;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Objects;

import android_serialport_api.SerialPort;

public class RfCardReaderReceive extends RfCardReader implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(RfCardReaderReceive.class);
    private static final int HEADER_SIZE = 9;
    private byte[] SIO_TX_Buff = new byte[12];

    private RfMode cmd = RfMode.RF_CARD_RELEASE;
    Thread receiveThread;

    private InputStream inputStream;
    private OutputStream outputStream;

    private boolean isOpen = false;
    private boolean endFlag = false;
    private byte[] src_data = new byte[8];
    int ch;

    public RfCardReaderReceive(String deviceComportName) {
        try {
            SerialPort serialPort = new SerialPort(new File(deviceComportName), 115200, 0);
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
            isOpen = true;
            receiveThread = new Thread(this);
            receiveThread.start();
        } catch (Exception e) {
            logger.error("rf card reader initial error : {}", e.getMessage());
        }
    }

    @Override
    public void rfCardReadRequest(int ch) {
        setCh(ch);
        send(RfMode.RF_CARD_CONTINUE_TMONEY);
    }

    @Override
    public void rfCardReadRelease() {
        send(RfMode.RF_CARD_RELEASE);
    }

    @Override
    public void destroyThread() {
        if (receiveThread != null) receiveThread.interrupt();
    }

    @Override
    public void stopThread() {
        isOpen = false;
        endFlag = true;
        receiveThread.interrupt();
    }

    @Override
    public void run() {
        int value = 0;
        byte[] readData = new byte[512];
        while (!endFlag && !Thread.currentThread().isInterrupted()) {
            try {
                Arrays.fill(readData, (byte)0x00);
                value = inputStream.available();
                if (value >= HEADER_SIZE) {
                    int readCnt = inputStream.read(readData, 0, value);
                } else {
                    Thread.sleep(10);
                    continue;
                }
                if (readData[0] != 0x02) continue;
                int len = BitUtilities.makeInt(readData[7], readData[8]);
                processCardData(readData, value,len-1);
            } catch (Exception e) {
                logger.error(e.getMessage());
                try {
                    Thread.sleep(100);
                } catch (Exception ez) {
                    logger.error(ez.getMessage());
                }
            }
        }
    }

    private void processCardData(byte[] readData, int responseSize, int dataStart) {
        String destData = null;
        Arrays.fill(src_data, (byte)0x00);
        if (checkSum(readData, responseSize) && Objects.equals(readData[0], (byte)0x02) &&
                Objects.equals(readData[responseSize-1], (byte)0x03)) {
            if (Objects.equals(readData[6], (byte)0xea)) {
                try {
                    // 카드 읽기
                    System.arraycopy(readData, dataStart, src_data, 0, 8);
                    destData = BitUtilities.byteArrayToString(src_data);
                    if (rfCardReaderListener != null) rfCardReaderListener.onRfCardDataReceive(getCh(), destData, true);
                } catch (Exception e) {
                    destData = "0000000000000000";
                    logger.error("processCardData error : {} " , e.getMessage());
                }
            } else if (Objects.equals(readData[6],(byte)0xeb)) {
                try {
                    //카드 읽기
                    System.arraycopy(readData, dataStart, src_data, 0,8);
                    destData = BitUtilities.byteArrayToString(src_data);
                    if (rfCardReaderListener != null) rfCardReaderListener.onRfCardDataReceive(getCh(),destData,true);
                } catch (Exception e) {
                    destData = "0000000000000000";
                    logger.error("rf processCardData error : {} " , e.getMessage());
                }
            }
        } else  {
            if (rfCardReaderListener != null) {
                rfCardReaderListener.onRfCardDataReceive(getCh(),"0000000000000000", false);
            }
        }
    }

    private boolean checkSum(byte[] response, int responseSize) {
        short crc = 0x00;
        byte crcHigh = 0x00, crcLow = 0x00;

        try {
            crc = CRC16.setCrc16(response, 0, responseSize-4);
            crcHigh = (byte)((crc >> 8) & 0xff);
            crcLow = (byte)(crc & 0xff);
            if (Objects.equals(crcHigh,response[responseSize-3]) && Objects.equals(crcLow,response[responseSize-2])) {
                return true;
            }
        } catch (Exception e) {
            logger.error("rf checkSum error : {}", e.getMessage());
        }
        return false;
    }

    private void send(RfMode rfCmd) {
        try {
            if (isOpen) {
                Arrays.fill(SIO_TX_Buff, (byte)0x00);
                SIO_Buff_Make(rfCmd);
                outputStream.write(SIO_TX_Buff);
            }
        } catch (Exception e) {
            logger.error("rf send error : {}", e.getMessage());
        }
    }
    private void SIO_Buff_Make(RfMode rfCmd) {
        short crc = 0x0000;
        SIO_TX_Buff[0] = 0x02;
        SIO_TX_Buff[1] = 0x01;
        SIO_TX_Buff[2] = 0x0b;
        SIO_TX_Buff[3] = 0x01;
        SIO_TX_Buff[4] = 0x0e;
        SIO_TX_Buff[5] = 0x01;

        switch (rfCmd) {
            case RF_CARD_RELEASE:
                SIO_TX_Buff[6] = (byte)(0xd6 & 0xff);
                break;
            case RF_CARD_VER:
                SIO_TX_Buff[6] = (byte)(0xd7 & 0xff);
                break;
            case RF_CARD_STATUS:
                SIO_TX_Buff[6] = (byte)(0xd8 & 0xff);
                break;
            case RF_CARD_SCAN:
                SIO_TX_Buff[6] = (byte)(0xeb & 0xff);
                break;
            case RF_CARD_CONTINUE_TMONEY:
                SIO_TX_Buff[6] = (byte)(0xea & 0xff);
                break;
        }
        SIO_TX_Buff[7] = SIO_TX_Buff[8] = 0x00;
        crc = CRC16.setCrc16(SIO_TX_Buff,0,8);
        SIO_TX_Buff[9] = (byte)((crc >> 8) & 0xff);
        SIO_TX_Buff[10] = (byte)(crc & 0xff);
        //ETX
        SIO_TX_Buff[11] = 0x03;
    }

    public int getCh() {
        return ch;
    }

    public void setCh(int ch) {
        this.ch = ch;
    }
}
