package com.dongah.fastcharger.basefunction;

import android.annotation.SuppressLint;
import android.os.Environment;
import android.text.TextUtils;

import com.dongah.fastcharger.utils.FileManagement;
import com.dongah.fastcharger.websocket.ocpp.firmware.DiagnosticsStatus;
import com.dongah.fastcharger.websocket.ocpp.firmware.FirmwareStatus;
import com.dongah.fastcharger.websocket.ocpp.security.SignedFirmwareStatus;
import com.dongah.fastcharger.websocket.ocpp.security.UploadLogStatus;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ChargerConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ChargerConfiguration.class);

    public static final String CONFIG_FILE_NAME = "config";
    FileManagement fileManagement;

    public String rootPath = "";

    public String MID = "KIOSK1114915545";      // 결제MID
    
    /** MAX Channel Count */
    public int maxChannel = 2;

    /**
     * server connection string
     * test server: ws://dev-connect.lselink.com/ocpp/00000026
     * ws://dev-connect.lselink.com/ocpp/{충전소ID}{충전기ID}
     * 충전소ID : 000000
     * 충전기ID : 26
     * */
    public String serverConnectingString = "ws://dev-connect.lselink.com/ocpp/";
    public int serverPort = 4000;

    /** 회원 인증 모드
     * 0: mac
     * 1: member
     * 2: mac + member
     * */
    public int authMode = 2;
    public int authModeId;

    /** 운영모드
     * 0: test
     * 1: server
     * */
    public int opMode = 0;
    public int opModeId;

    /** 시작 모드
     * 0: 터치 시작 모드
     * 1: 자동 시작 모드
     * */
    public int startMode = 0;

    /** device serial port */
    public String controlCom = "/dev/ttyS7";
    public String rfCom = "/dev/ttyS3";
    public String creditCom = "/dev/ttyS4";

    /** charging point configuration setting */
    public int chargerPointType = 0;                    // 커플러 타입
    public String chargerPointModel = "DEVD240";        // 충전기 모델ID
    public int chargerPointModelCode = 0;
    public String chargeBoxSerialNumber = "";           // 충전소ID
    public String chargerId = "";                       // 충전기ID
    public String chargePointSerialNumber = "";         // 충전기 시리얼 번호
    public String chargePointVendor = "DONGAH";         // 충전기 vendor(제조사 코드)
    public String firmwareVersion = "";                 // 펌웨어 버전
    public String iccid = "";                           // 모뎀 SIM 카드의 ICCID
    public String imsi = "";                            // 모뎀 SIM 카드의 IMSI
    public String meterSerialNumber = "";               // 충전기의 주전력량계의 시리얼 번호
    public String meterType = "";                       // 충전기의 주전력량계의 타입 포함
    public int connectorPriority = 0;                   // 1구 제어 우선순위
    public String testPrice = "313.0";                  // 테스트 단가
    public int targetSoc = 80;                          // SoC
    public int dr = 0;                                  // 전류 제한

    public boolean StopConfirm;
    public boolean signed = true ;
    public boolean controlMonitor = true;
    public boolean initInfo = true;
    public FirmwareStatus firmwareStatus = FirmwareStatus.Idle;
    public SignedFirmwareStatus signedFirmwareStatus = SignedFirmwareStatus.Idle;
    public DiagnosticsStatus diagnosticsStatus = DiagnosticsStatus.Idle;
    public UploadLogStatus uploadLogStatus = UploadLogStatus.Idle;

    public ChargerConfiguration() {
        setRootPath(Environment.getExternalStorageDirectory().toString() + File.separator + "Download");
        fileManagement = new FileManagement();
    }

    @SuppressLint("SetWorldReadable")
    public void onLoadConfiguration() {
        try {
            File targetFile = new File(GlobalVariables.ROOT_PATH + File.separator + CONFIG_FILE_NAME);
            String configurationString;
            if (!targetFile.exists()) onSaveConfiguration();

//            targetFile.setReadable(true, false);
//            targetFile.setWritable(true, false);

            // get file context json string
            configurationString = fileManagement.getStringFromFile(GlobalVariables.ROOT_PATH  + File.separator + CONFIG_FILE_NAME);
            if (!TextUtils.isEmpty(configurationString)) {
                JSONObject obj = new JSONObject(configurationString);
                setChargerId(obj.getString("CHARGER_ID"));
                setServerConnectingString(obj.getString("SERVER_CONNECTING_STRING"));
                setServerPort(obj.getInt("SERVER_PORT"));
                setAuthMode(obj.getInt("AUTH_MODE"));
                setAuthModeId(obj.getInt("AUTH_MODE_ID"));
                setOpMode(obj.getInt("OP_MODE"));
                setOpModeId(obj.getInt("OP_MODE_ID"));
                setControlCom(obj.getString("CONTROL_COM"));
                setRfCom(obj.getString("RF_COM"));
                setCreditCom(obj.getString("CREDIT_COM"));
                setTestPrice(obj.getString("TEST_PRICE"));
                setChargerPointType(obj.getInt("CHARGER_POINT_TYPE"));
                setChargePointVendor(obj.getString("CHARGE_POINT_VENDOR"));
                setChargerPointModel(obj.getString("CHARGER_POINT_MODEL"));
                setChargerPointModelCode(obj.getInt("CHARGER_POINT_MODEL_CODE"));
                setChargeBoxSerialNumber(obj.getString("CHARGE_BOX_SERIAL_NUMBER"));
                setChargePointSerialNumber(obj.getString("CHARGE_POINT_SERIAL_NUMBER"));
                setFirmwareVersion(obj.getString("FIRMWARE_VERSION"));
                setIccid(obj.getString("ICCID"));
                setImsi(obj.getString("IMSI"));
                setMeterSerialNumber(obj.getString("METER_SERIAL_NUMBER"));
                setMeterType(obj.getString("METER_TYPE"));
                setConnectorPriority(obj.getInt("CONNECTOR_PRIORITY"));
                setStopConfirm(obj.getBoolean("STOP_CONFIRM"));
                setTargetSoc(obj.getInt("TARGET_SOC"));
                setDr(obj.getInt("DR"));
                setStartMode(obj.getInt("START_MODE"));
                setSigned(obj.getBoolean("SIGNED"));
                setControlMonitor(obj.getBoolean("CONTROL_MONITOR"));
                setInitInfo(obj.getBoolean("INIT_INFO"));
            }
        } catch (Exception e) {
            logger.error("configuration load fail: {}", e.getMessage(), e);
        }
    }

    public void onSaveConfiguration() {
        try {
            JSONObject obj = new JSONObject();
            obj.put("CHARGER_ID", getChargerId());
            obj.put("SERVER_CONNECTING_STRING", getServerConnectingString());
            obj.put("SERVER_PORT", getServerPort());
            obj.put("AUTH_MODE", getAuthMode());
            obj.put("AUTH_MODE_ID", getAuthModeId());
            obj.put("OP_MODE", getOpMode());
            obj.put("OP_MODE_ID", getOpModeId());
            obj.put("CONTROL_COM", getControlCom());
            obj.put("RF_COM", getRfCom());
            obj.put("CREDIT_COM", getCreditCom());
            obj.put("TEST_PRICE", getTestPrice());
            obj.put("CHARGER_POINT_TYPE", getChargerPointType());
            obj.put("CHARGE_POINT_VENDOR", getChargePointVendor());
            obj.put("CHARGER_POINT_MODEL", getChargerPointModel());
            obj.put("CHARGER_POINT_MODEL_CODE", getChargerPointModelCode());
            obj.put("CHARGE_BOX_SERIAL_NUMBER", getChargeBoxSerialNumber());
            obj.put("CHARGE_POINT_SERIAL_NUMBER", getChargePointSerialNumber());
            obj.put("FIRMWARE_VERSION", getFirmwareVersion());
            obj.put("ICCID", getIccid());
            obj.put("IMSI", getImsi());
            obj.put("METER_SERIAL_NUMBER", getMeterSerialNumber());
            obj.put("METER_TYPE", getMeterType());
            obj.put("CONNECTOR_PRIORITY", getConnectorPriority());
            obj.put("STOP_CONFIRM", isStopConfirm());
            obj.put("TARGET_SOC", getTargetSoc());
            obj.put("DR", getDr());
            obj.put("START_MODE", getStartMode());
            obj.put("SIGNED", isSigned());
            obj.put("CONTROL_MONITOR", isControlMonitor());
            obj.put("INIT_INFO", isInitInfo());
            fileManagement.stringToFileSave(rootPath, CONFIG_FILE_NAME, obj.toString(), false);
        } catch (Exception e) {
            logger.error("configuration save fail: {}", e.getMessage(), e);
        }
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getMID() {
        return MID;
    }

    public void setMID(String MID) {
        this.MID = MID;
    }

    public int getMaxChannel() {
        return maxChannel;
    }

    public void setMaxChannel(int maxChannel) {
        this.maxChannel = maxChannel;
    }

    public String getChargerId() {
        return chargerId;
    }

    public void setChargerId(String chargerId) {
        this.chargerId = chargerId;
    }

    public String getServerConnectingString() {
        return serverConnectingString;
    }

    public void setServerConnectingString(String serverConnectingString) {
        this.serverConnectingString = serverConnectingString;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public int getAuthMode() {
        return authMode;
    }

    public void setAuthMode(int authMode) {
        this.authMode = authMode;
    }

    public int getAuthModeId() {
        return authModeId;
    }

    public void setAuthModeId(int authModeId) {
        this.authModeId = authModeId;
    }

    public int getOpMode() {
        return opMode;
    }

    public void setOpMode(int opMode) {
        this.opMode = opMode;
    }

    public int getOpModeId() {
        return opModeId;
    }

    public void setOpModeId(int opModeId) {
        this.opModeId = opModeId;
    }

    public int getStartMode() {
        return startMode;
    }

    public void setStartMode(int startMode) {
        this.startMode = startMode;
    }

    public String getControlCom() {
        return controlCom;
    }

    public void setControlCom(String controlCom) {
        this.controlCom = controlCom;
    }

    public String getRfCom() {
        return rfCom;
    }

    public void setRfCom(String rfCom) {
        this.rfCom = rfCom;
    }

    public String getCreditCom() {
        return creditCom;
    }

    public void setCreditCom(String creditCom) {
        this.creditCom = creditCom;
    }

    public int getChargerPointType() {
        return chargerPointType;
    }

    public void setChargerPointType(int chargerPointType) {
        this.chargerPointType = chargerPointType;
    }

    public String getChargePointVendor() {
        return chargePointVendor;
    }

    public void setChargePointVendor(String chargePointVendor) {
        this.chargePointVendor = chargePointVendor;
    }

    public String getChargerPointModel() {
        return chargerPointModel;
    }

    public void setChargerPointModel(String chargerPointModel) {
        this.chargerPointModel = chargerPointModel;
    }

    public int getChargerPointModelCode() {
        return chargerPointModelCode;
    }

    public void setChargerPointModelCode(int chargerPointModelCode) {
        this.chargerPointModelCode = chargerPointModelCode;
    }

    public String getChargeBoxSerialNumber() {
        return chargeBoxSerialNumber;
    }

    public void setChargeBoxSerialNumber(String chargeBoxSerialNumber) {
        this.chargeBoxSerialNumber = chargeBoxSerialNumber;
    }

    public String getChargePointSerialNumber() {
        return chargePointSerialNumber;
    }

    public void setChargePointSerialNumber(String chargePointSerialNumber) {
        this.chargePointSerialNumber = chargePointSerialNumber;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getIccid() {
        return iccid;
    }

    public void setIccid(String iccid) {
        this.iccid = iccid;
    }

    public String getImsi() {
        return imsi;
    }

    public void setImsi(String imsi) {
        this.imsi = imsi;
    }

    public String getMeterSerialNumber() {
        return meterSerialNumber;
    }

    public void setMeterSerialNumber(String meterSerialNumber) {
        this.meterSerialNumber = meterSerialNumber;
    }

    public String getMeterType() {
        return meterType;
    }

    public void setMeterType(String meterType) {
        this.meterType = meterType;
    }

    public int getConnectorPriority() {
        return connectorPriority;
    }

    public void setConnectorPriority(int connectorPriority) {
        this.connectorPriority = connectorPriority;
    }

    public String getTestPrice() {
        return testPrice;
    }

    public void setTestPrice(String testPrice) {
        this.testPrice = testPrice;
    }

    public boolean isStopConfirm() {
        return StopConfirm;
    }

    public void setStopConfirm(boolean stopConfirm) {
        this.StopConfirm = stopConfirm;
    }

    public boolean isSigned() {
        return signed;
    }

    public void setSigned(boolean signed) {
        this.signed = signed;
    }

    public boolean isControlMonitor() {
        return controlMonitor;
    }

    public void setControlMonitor(boolean controlMonitor) {
        this.controlMonitor = controlMonitor;
    }

    public boolean isInitInfo() {
        return initInfo;
    }

    public void setInitInfo(boolean initInfo) {
        this.initInfo = initInfo;
    }

    public FirmwareStatus getFirmwareStatus() {
        return firmwareStatus;
    }

    public void setFirmwareStatus(FirmwareStatus firmwareStatus) {
        this.firmwareStatus = firmwareStatus;
    }

    public DiagnosticsStatus getDiagnosticsStatus() {
        return diagnosticsStatus;
    }

    public void setDiagnosticsStatus(DiagnosticsStatus diagnosticsStatus) {
        this.diagnosticsStatus = diagnosticsStatus;
    }

    public UploadLogStatus getUploadLogStatus() {
        return uploadLogStatus;
    }

    public void setUploadLogStatus(UploadLogStatus uploadLogStatus) {
        this.uploadLogStatus = uploadLogStatus;
    }

    public SignedFirmwareStatus getSignedFirmwareStatus() {
        return signedFirmwareStatus;
    }

    public void setSignedFirmwareStatus(SignedFirmwareStatus signedFirmwareStatus) {
        this.signedFirmwareStatus = signedFirmwareStatus;
    }

    public int getTargetSoc() {
        return targetSoc;
    }

    public void setTargetSoc(int targetSoc) {
        this.targetSoc = targetSoc;
    }

    public int getDr() {
        return dr;
    }

    public void setDr(int dr) {
        this.dr = dr;
    }
}
