package com.dongah.fastcharger.basefunction;

import android.os.Environment;

import com.dongah.fastcharger.websocket.ocpp.localauthlist.UpdateStatus;

import java.io.File;

public class GlobalVariables {

    //storage/emulated/0/download
    public static String ROOT_PATH = Environment.getExternalStorageDirectory().toString() + File.separator + "Download";

    public static String VERSION = "1.2.0";
    public static String FW_VERSION = "1.0.1";

    // file names
    public static final String UNIT_FILE_NAME = "unitPrice.dongah";
    public static final String FILE_UNIT = "unitPrice";
    public static final String FILE_CHANGE_MODE = "changeMode";
    public static final String FILE_CHANGE_ELEC_MODE = "changeElecMode";
    public static final String FILE_FULL_RECHG_SOC = "fullRechgSoc";
    public static final String FILE_RECHGR_SOC_SCHEDULE = "rechgrsocschedule";

    // unitPrice
    public static double userTypeC = 0;    // 법인 단가
    public static double userTypeK = 0;    // 환경부 단가
    public static double userTypeM = 0;    // 회원 단가
    public static double userTypeN = 0;    // 비회원 단가


    /**
     * Max plug count
     */
    public static int maxChannel = 2;
    public static int maxPlugCount = 3;
    public static boolean CONNECT_RETRY = false;
    public static boolean[] ChargerOperation = new boolean[maxPlugCount];
    public static UpdateStatus updateStatus;

    /**
     * ocpp configuration key
     */
    public static boolean notSupportedKey = false;
    public static int ConnectionTimeOut = 60;
    public static int MinimunStatusDuration = 300;
    public static int MeterValueSampleInterval = 60;
    public static int HeartBeatInterval = 60;
    public static boolean AuthorizeRemoteTxRequests = false;
    public static boolean ReserveConnectorZeroSupported = true;
    public static boolean LocalPreAuthorize = false;
    public static boolean AllowOfflineTxForUnknownId = false;
    public static boolean AuthorizationCacheEnabled = false;
    public static boolean StopTransactionOnInvalidId = false;
    public static boolean StopTransactionOnEVSideDisconnect = true;
    public static boolean UnlockConnectorOnEVSideDisconnect = true;
    public static int ClockAlignedDataInterval = 0;
    public static boolean LocalAuthorizeOffline = false;
    public static String SecurityProfile = "0";
    public static String AuthorizationKey = "";
    public static boolean LocalAuthListEnabled = true;

    // LS E-Link Configuration Key
    public static int FullRechgAmt = 40000;
    public static  String PersonUtztnLmtYn = "N";
    public static int PersonUtztnLmtHr = 30;
    public static String webSocketURL = "";
    public static boolean UseBasicAuth = false;

    public static int[] dumpTransactionId = new int[2];
    public static boolean[] dumpSending = new boolean[2];
    public static boolean reconnectCheck = false;
    public static boolean Scheduled = false;

    //modem tel number
    public static String IMSI = "" ;
    public static String RSRP = "";
    public static boolean triggerSet = false;




    public static String getRootPath() {
        return ROOT_PATH;
    }

    public static void setRootPath(String rootPath) {
        ROOT_PATH = rootPath;
    }

    public static boolean isConnectRetry() {
        return CONNECT_RETRY;
    }

    public static void setConnectRetry(boolean connectRetry) {
        CONNECT_RETRY = connectRetry;
    }

    public static boolean isNotSupportedKey() {
        return notSupportedKey;
    }

    public static void setNotSupportedKey(boolean notSupportedKey) {
        GlobalVariables.notSupportedKey = notSupportedKey;
    }

    public static String getAuthorizationKey() {
        return AuthorizationKey;
    }

    public static void setAuthorizationKey(String authorizationKey) {
        AuthorizationKey = authorizationKey;
    }

    public static boolean isLocalAuthListEnabled() {
        return LocalAuthListEnabled;
    }

    public static void setLocalAuthListEnabled(boolean localAuthListEnabled) {
        LocalAuthListEnabled = localAuthListEnabled;
    }


    public static int getConnectionTimeOut() {
        return ConnectionTimeOut;
    }

    public static void setConnectionTimeOut(int connectionTimeOut) {
        ConnectionTimeOut = connectionTimeOut;
    }

    public static int getMinimunStatusDuration() {
        return MinimunStatusDuration;
    }

    public static void setMinimunStatusDuration(int minimunStatusDuration) {
        MinimunStatusDuration = minimunStatusDuration;
    }

    public static int getMeterValueSampleInterval() {
        return MeterValueSampleInterval;
    }

    public static void setMeterValueSampleInterval(int meterValueSampleInterval) {
        MeterValueSampleInterval = meterValueSampleInterval;
    }

    public static int getHeartBeatInterval() {
        return HeartBeatInterval;
    }

    public static void setHeartBeatInterval(int heartBeatInterval) {
        HeartBeatInterval = heartBeatInterval;
    }

    public static boolean isAuthorizeRemoteTxRequests() {
        return AuthorizeRemoteTxRequests;
    }

    public static void setAuthorizeRemoteTxRequests(boolean authorizeRemoteTxRequests) {
        AuthorizeRemoteTxRequests = authorizeRemoteTxRequests;
    }

    public static boolean isReserveConnectorZeroSupported() {
        return ReserveConnectorZeroSupported;
    }

    public static void setReserveConnectorZeroSupported(boolean reserveConnectorZeroSupported) {
        ReserveConnectorZeroSupported = reserveConnectorZeroSupported;
    }

    public static boolean isLocalPreAuthorize() {
        return LocalPreAuthorize;
    }

    public static void setLocalPreAuthorize(boolean localPreAuthorize) {
        LocalPreAuthorize = localPreAuthorize;
    }

    public static boolean isAuthorizationCacheEnabled() {
        return AuthorizationCacheEnabled;
    }

    public static void setAuthorizationCacheEnabled(boolean authorizationCacheEnabled) {
        AuthorizationCacheEnabled = authorizationCacheEnabled;
    }

    public static boolean isAllowOfflineTxForUnknownId() {
        return AllowOfflineTxForUnknownId;
    }

    public static void setAllowOfflineTxForUnknownId(boolean allowOfflineTxForUnknownId) {
        AllowOfflineTxForUnknownId = allowOfflineTxForUnknownId;
    }

    public static boolean isStopTransactionOnInvalidId() {
        return StopTransactionOnInvalidId;
    }

    public static void setStopTransactionOnInvalidId(boolean stopTransactionOnInvalidId) {
        StopTransactionOnInvalidId = stopTransactionOnInvalidId;
    }

    public static boolean isStopTransactionOnEVSideDisconnect() {
        return StopTransactionOnEVSideDisconnect;
    }

    public static void setStopTransactionOnEVSideDisconnect(boolean stopTransactionOnEVSideDisconnect) {
        StopTransactionOnEVSideDisconnect = stopTransactionOnEVSideDisconnect;
    }

    public static boolean isUnlockConnectorOnEVSideDisconnect() {
        return UnlockConnectorOnEVSideDisconnect;
    }

    public static void setUnlockConnectorOnEVSideDisconnect(boolean unlockConnectorOnEVSideDisconnect) {
        UnlockConnectorOnEVSideDisconnect = unlockConnectorOnEVSideDisconnect;
    }

    public static int getClockAlignedDataInterval() {
        return ClockAlignedDataInterval;
    }

    public static void setClockAlignedDataInterval(int clockAlignedDataInterval) {
        ClockAlignedDataInterval = clockAlignedDataInterval;
    }

    public static boolean isLocalAuthorizeOffline() {
        return LocalAuthorizeOffline;
    }

    public static void setLocalAuthorizeOffline(boolean localAuthorizeOffline) {
        LocalAuthorizeOffline = localAuthorizeOffline;
    }

    public static String getSecurityProfile() {
        return SecurityProfile;
    }

    public static void setSecurityProfile(String securityProfile) {
        SecurityProfile = securityProfile;
    }

    public static int getFullRechgAmt() {
        return FullRechgAmt;
    }

    public static void setFullRechgAmt(int fullRechgAmt) {
        FullRechgAmt = fullRechgAmt;
    }

    public static String getPersonUtztnLmtYn() {
        return PersonUtztnLmtYn;
    }

    public static void setPersonUtztnLmtYn(String personUtztnLmtYn) {
        PersonUtztnLmtYn = personUtztnLmtYn;
    }

    public static int getPersonUtztnLmtHr() {
        return PersonUtztnLmtHr;
    }

    public static void setPersonUtztnLmtHr(int personUtztnLmtHr) {
        PersonUtztnLmtHr = personUtztnLmtHr;
    }

    public static String getWebSocketURL() {
        return webSocketURL;
    }

    public static void setWebSocketURL(String url) {
        webSocketURL = url;
    }

    public static boolean isUseBasicAuth() {
        return UseBasicAuth;
    }

    public static void setUseBasicAuth(boolean useBasicAuth) {
        UseBasicAuth = useBasicAuth;
    }

    public static int getDumpTransactionId(int connectorId) {
        if (connectorId > 0 && connectorId <= maxChannel) {
            return dumpTransactionId[connectorId - 1];
        }
        return 0;
    }

    public static void setDumpTransactionId(int connectorId, int dumpTransactionIdValue) {
        if (connectorId > 0 && connectorId <= maxChannel) {
            dumpTransactionId[connectorId - 1] = dumpTransactionIdValue;
        }
    }

    public static boolean isDumpSending(int connectorId)
    {
        return dumpSending[connectorId-1];
    }

    public static void setDumpSending(int connectorId, boolean dumpSending) {
        GlobalVariables.dumpSending[connectorId - 1] = dumpSending;
    }

    public static boolean isReconnectCheck() {
        return reconnectCheck;
    }

    public static void setReconnectCheck(boolean reconnectCheck) {
        GlobalVariables.reconnectCheck = reconnectCheck;
    }

    public static boolean isScheduled() {
        return Scheduled;
    }

    public static void setScheduled(boolean scheduled) {
        Scheduled = scheduled;
    }

    public static String getIMSI() {
        return IMSI;
    }

    public static void setIMSI(String IMSI) {
        GlobalVariables.IMSI = IMSI;
    }

    public static String getRSRP() {
        return RSRP;
    }

    public static void setRSRP(String RSRP) {
        GlobalVariables.RSRP = RSRP;
    }

    public static boolean isTriggerSet() {
        return triggerSet;
    }

    public static void setTriggerSet(boolean triggerSet) {
        GlobalVariables.triggerSet = triggerSet;
    }
}
