package com.dongah.fastcharger.basefunction;

import com.dongah.fastcharger.websocket.ocpp.core.ChargePointErrorCode;
import com.dongah.fastcharger.websocket.ocpp.core.ChargePointStatus;
import com.dongah.fastcharger.websocket.ocpp.core.Reason;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChargingCurrentData {

    private static final Logger logger = LoggerFactory.getLogger(ChargingCurrentData.class);

    int connectorId = 0;
    int transactionId = 0;

    long integratedPower = 0;
    /**
     * reset 받으면 reboot = true 설정
     */
    public boolean reBoot = false;

    ChargerPointType chargerPointType = ChargerPointType.NONE;

    ChargePointStatus chargePointStatus = ChargePointStatus.Available;
    ChargePointErrorCode chargePointErrorCode = ChargePointErrorCode.NoError;

    Reason stopReason = Reason.Other;
    boolean userStop = false;

    String idTag = "";
    String parentIdTag = "";
    String idTagStop = "";
    String parentIdTagStop = "";
    /**
     * pay type : MEMBER(1) CREDIT(2), TEST(3), FREE(4)
     */
    PaymentType paymentType = PaymentType.NONE;
    public String smsTelNum = "";
    double powerUnitPrice = 0;         //충전단가
    String chargingStartTime = "";
    String chargingEndTime = "";
    long chargingTime = 0;
    String chargingUseTime = "";
    long powerMeter = 0;
    long powerMeterStart = 0;
    long powerMeterStop = 0;

    long powerMeterCalculate = 0;
    long powerMeterUse = 0;
    double powerMeterUsePay = 0f;
    double outPutVoltage = 0f;
    double outPutCurrent = 0f;
    double Frequency = 0f;
    double targetCurrent = 0f;      // 요청전류

    int soc = 0;
    int targetSoc = 80;
    long chargingRemainTime = 0;

    /**
     * 신용 카드 결제
     */
    String tradeCode;
    String tradeMethod;
    int prePayment = 0;
    int partialCancelPayment = 0;
    int surtax = 0;
    int tip = 0;            // 봉사료 (비과세) 금액
    int installment = 0;    // 할부 개월

    String authNum;
    String phoneNumber;
    String errorType;               // 0: no error      3:부분 취소 미처리건 있음;
    String agency;                  // VAN사 SMATRO
    String limitApplyMID;           // 상점 ID
    String limitApplyTid;           // 한도 승인 거래 번호
    String limitApplySeq;           // 한도 승인 거래 일련 번호
    String limitApplyNumber;        // 한도 승인 거래 승인 번호
    int usePoint;                   // 부분 취소할 금액
    int limitApplyAmount;           // 한도 승인 금액
    String limitApplyDt;            // 한도 승인 일시 YYYY-MM-DDHH:mm:ssZ
    int chargeIdx;                  // CMS 인덱스
    String partCancelTid;
    String partCancelNumber;
    int partCancelAmount;
    String partCancelDt;

    int remaintime = 0;

    String approvalNo = "";
    String approvalDate = "";
    String approvalTime = "";
    String pgTranSeq = "";          // TID(결제승인관리번호)
    String creditNo = "";

    String approvalNumber = "";
    String creditCardNumber = "";
    String responseCode = "";       //응답코드
    String responseMessage = "";    //응답메세지
    String payId = "";              //충전기에서 30자리로 생성하여 Start Transaction Id로 사용
    String cancelApprovalNo = "";
    String cancelApprovalDate = "";
    String cancelApprovalTime = "";
    String cancelPgTranSeq = "";
    String cancelUniqueNo = "";              //취소 거래고유번호
    String tradeUniqueNumber = "";           //거래고유번호
    String issuer = "";                      //발급사 정보
    String buyer = "";                       //매입사
    String terminalNumber = "";              //장치제품번호
    String storeNumber = "";                 //가맹점번호


    String remainTimeStr = "";


    public SampleValueData sampleValueData;
    boolean authorizeResult = false;
    boolean prePaymentResult = false;       //선결제 성공 여부  true : 성공
    boolean faultCancelPayment = false;

    StringBuilder faultMessage;
    ChargePointStatus reservedStatus = ChargePointStatus.Available;

    /**
     * reserve now
     */
    int resConnectorId = 0;
    String resExpiryDate = "";
    String resIdTag = "";
    String resParentIdTag = "";
    String resReservationId = "";

    /**
     * 인증타입
     * C: 법인
     * K: 환경부
     * M: 회원카드
     * N: 신용카드
     * */
    public String authType = "M";

    /**
     * 커넥터 상태
     * DM: 양구
     * NM: 1구
     * WM: 충전 대기
     * IM: 충전 불가
     * */
    public String changeMode = "DM";

    /** 커넥터 사용 유무
     * true: 사용, false: 미사용
     * */
    public boolean connectUse = true;

    public int limitSoc = 0;
    public short limitPower = 0;
    public int fullrechgsoc = 100;

    public ChargingCurrentData() {
        sampleValueData = new SampleValueData();
    }

    public void onCurrentDataClear() {
        try {
            sampleValueData.initSampledValues();
            setIdTag("");
            setPayId("");
            setUserStop(false);
            setPowerMeterStart(0);
            setPowerMeterUse(0);
            setPowerMeterUsePay(0);
            setPowerUnitPrice(0);
            setOutPutVoltage(0);
            setOutPutCurrent(0);
            setPowerMeterStop(0);
            setPowerMeterCalculate(0);
            setTradeCode("");
            setTradeMethod("");
            setPrePayment(0);
            setPartialCancelPayment(0);
            setApprovalNumber("");
            setApprovalDate("");
            setApprovalTime("");
            setPgTranSeq("");
            setCreditCardNumber("");
            setStoreNumber("");
            setTerminalNumber("");
            setResponseCode("");
            setResponseMessage("");
            setPrePaymentResult(false);
            setTradeUniqueNumber("");
            setSurtax(0);
            setTip(0);
            setInstallment(0);
            setIssuer("");
            setBuyer("");
            setChargingStartTime("");
            setChargingEndTime("");
            setTransactionId(0);
            setChargerPointType(ChargerPointType.NONE);
            setPaymentType(PaymentType.NONE);
            setSmsTelNum(null);
            setChargingTime(0);
            setFaultCancelPayment(false);
            setAuthorizeResult(false);
            setRemaintime(0);
            setParentIdTag("");
            setIdTagStop("");
            setParentIdTagStop("");
//            setResExpiryDate("");
//            setResIdTag("");
//            setResParentIdTag("");
//            setResReservationId("");
            setSoc(0);
            setTargetSoc(80);
            setTargetCurrent(0);
            setAuthType("M");
            setFullrechgsoc(100);
            setStopReason(Reason.Other);
        } catch (Exception e) {
            logger.error("ChargingCurrentData onCurrentDataClear error : {}", e.getMessage(), e);
        }
    }

    public int getTargetSoc() {
        return targetSoc;
    }

    public void setTargetSoc(int targetSoc) {
        this.targetSoc = targetSoc;
    }


    public int getConnectorId() {
        return connectorId;
    }

    public void setConnectorId(int connectorId) {
        this.connectorId = connectorId;
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public long getIntegratedPower() {
        return integratedPower;
    }

    public void setIntegratedPower(long integratedPower) {
        this.integratedPower = integratedPower;
    }

    public boolean isReBoot() {
        return reBoot;
    }

    public void setReBoot(boolean reBoot) {
        this.reBoot = reBoot;
    }

    public ChargerPointType getChargerPointType() {
        return chargerPointType;
    }

    public void setChargerPointType(ChargerPointType chargerPointType) {
        this.chargerPointType = chargerPointType;
    }

    public String getSmsTelNum() {
        return smsTelNum;
    }

    public void setSmsTelNum(String smsTelNum) {
        this.smsTelNum = smsTelNum;
    }

    public ChargePointStatus getChargePointStatus() {
        return chargePointStatus;
    }

    public void setChargePointStatus(ChargePointStatus chargePointStatus) {
        this.chargePointStatus = chargePointStatus;
    }

    public ChargePointErrorCode getChargePointErrorCode() {
        return chargePointErrorCode;
    }

    public void setChargePointErrorCode(ChargePointErrorCode chargePointErrorCode) {
        this.chargePointErrorCode = chargePointErrorCode;
    }

    public Reason getStopReason() {
        return stopReason;
    }

    public void setStopReason(Reason stopReason) {
        this.stopReason = stopReason;
    }

    public boolean isUserStop() {
        return userStop;
    }

    public void setUserStop(boolean userStop) {
        this.userStop = userStop;
    }

    public String getIdTag() {
        return idTag;
    }

    public void setIdTag(String idTag) {
        this.idTag = idTag;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public double getPowerUnitPrice() {
        return powerUnitPrice;
    }

    public void setPowerUnitPrice(double powerUnitPrice) {
        this.powerUnitPrice = powerUnitPrice;
    }

    public String getTradeCode() {
        return tradeCode;
    }

    public void setTradeCode(String tradeCode) {
        this.tradeCode = tradeCode;
    }

    public String getTradeMethod() {
        return tradeMethod;
    }

    public void setTradeMethod(String tradeMethod) {
        this.tradeMethod = tradeMethod;
    }

    public int getPrePayment() {
        return prePayment;
    }

    public void setPrePayment(int prePayment) {
        this.prePayment = prePayment;
    }

    public int getSurtax() {
        return surtax;
    }

    public void setSurtax(int surtax) {
        this.surtax = surtax;
    }

    public int getTip() {
        return tip;
    }

    public void setTip(int tip) {
        this.tip = tip;
    }

    public int getInstallment() {
        return installment;
    }

    public void setInstallment(int installment) {
        this.installment = installment;
    }

    public long getChargingTime() {
        return chargingTime;
    }

    public String getChargingStartTime() {
        return chargingStartTime;
    }

    public void setChargingStartTime(String chargingStartTime) {
        this.chargingStartTime = chargingStartTime;
    }

    public String getChargingEndTime() {
        return chargingEndTime;
    }

    public void setChargingEndTime(String chargingEndTime) {
        this.chargingEndTime = chargingEndTime;
    }

    public void setChargingTime(long chargingTime) {
        this.chargingTime = chargingTime;
    }

    public String getChargingUseTime() {
        return chargingUseTime;
    }

    public void setChargingUseTime(String chargingUseTime) {
        this.chargingUseTime = chargingUseTime;
    }

    public long getPowerMeter() {
        return powerMeter;
    }

    public void setPowerMeter(long powerMeter) {
        this.powerMeter = powerMeter;
    }

    public long getPowerMeterStart() {
        return powerMeterStart;
    }

    public void setPowerMeterStart(long powerMeterStart) {
        this.powerMeterStart = powerMeterStart;
    }

    public long getPowerMeterStop() {
        return powerMeterStop;
    }

    public void setPowerMeterStop(long powerMeterStop) {
        this.powerMeterStop = powerMeterStop;
    }

    public long getPowerMeterCalculate() {
        return powerMeterCalculate;
    }

    public void setPowerMeterCalculate(long powerMeterCalculate) {
        this.powerMeterCalculate = powerMeterCalculate;
    }

    public long getPowerMeterUse() {
        return powerMeterUse;
    }

    public void setPowerMeterUse(long powerMeterUse) {
        this.powerMeterUse = powerMeterUse;
    }

    public double getPowerMeterUsePay() {
        return powerMeterUsePay;
    }

    public void setPowerMeterUsePay(double powerMeterUsePay) {
        this.powerMeterUsePay = powerMeterUsePay;
    }

    public double getOutPutVoltage() {
        return outPutVoltage;
    }

    public void setOutPutVoltage(double outPutVoltage) {
        this.outPutVoltage = outPutVoltage;
    }

    public double getOutPutCurrent() {
        return outPutCurrent;
    }

    public void setOutPutCurrent(double outPutCurrent) {
        this.outPutCurrent = outPutCurrent;
    }

    public double getFrequency() {
        return Frequency;
    }

    public void setFrequency(double frequency) {
        Frequency = frequency;
    }

    public double getTargetCurrent() {
        return targetCurrent;
    }

    public void setTargetCurrent(double targetCurrent) {
        this.targetCurrent = targetCurrent;
    }

    public int getSoc() {
        return soc;
    }

    public void setSoc(int soc) {
        this.soc = soc;
    }

    public long getChargingRemainTime() {
        return chargingRemainTime;
    }

    public void setChargingRemainTime(long chargingRemainTime) {
        this.chargingRemainTime = chargingRemainTime;
    }

    public void setPartialCancelPayment(int partialCancelPayment) {
        this.partialCancelPayment = partialCancelPayment;
    }


    public int getPartialCancelPayment() {
        return partialCancelPayment;
    }

    public String getApprovalNumber() {
        return approvalNumber;
    }

    public void setApprovalNumber(String approvalNumber) {
        this.approvalNumber = approvalNumber;
    }

    public String getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(String approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getApprovalTime() {
        return approvalTime;
    }

    public void setApprovalTime(String approvalTime) {
        this.approvalTime = approvalTime;
    }

    public String getPgTranSeq() {
        return pgTranSeq;
    }

    public void setPgTranSeq(String pgTranSeq) {
        this.pgTranSeq = pgTranSeq;
    }

    public String getCreditCardNumber() {
        return creditCardNumber;
    }

    public void setCreditCardNumber(String creditCardNumber) {
        this.creditCardNumber = creditCardNumber;
    }


    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public String getPayId() {
        return payId;
    }

    public void setPayId(String payId) {
        this.payId = payId;
    }

    public String getCancelApprovalNo() {
        return cancelApprovalNo;
    }

    public void setCancelApprovalNo(String cancelApprovalNo) {
        this.cancelApprovalNo = cancelApprovalNo;
    }

    public String getCancelApprovalDate() {
        return cancelApprovalDate;
    }

    public void setCancelApprovalDate(String cancelApprovalDate) {
        this.cancelApprovalDate = cancelApprovalDate;
    }

    public String getCancelApprovalTime() {
        return cancelApprovalTime;
    }

    public void setCancelApprovalTime(String cancelApprovalTime) {
        this.cancelApprovalTime = cancelApprovalTime;
    }

    public String getCancelPgTranSeq() {
        return cancelPgTranSeq;
    }

    public void setCancelPgTranSeq(String cancelPgTranSeq) {
        this.cancelPgTranSeq = cancelPgTranSeq;
    }

    public String getCancelUniqueNo() {
        return cancelUniqueNo;
    }

    public void setCancelUniqueNo(String cancelUniqueNo) {
        this.cancelUniqueNo = cancelUniqueNo;
    }

    public String getTradeUniqueNumber() {
        return tradeUniqueNumber;
    }

    public void setTradeUniqueNumber(String tradeUniqueNumber) {
        this.tradeUniqueNumber = tradeUniqueNumber;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getBuyer() {
        return buyer;
    }

    public void setBuyer(String buyer) {
        this.buyer = buyer;
    }

    public String getTerminalNumber() {
        return terminalNumber;
    }

    public void setTerminalNumber(String terminalNumber) {
        this.terminalNumber = terminalNumber;
    }

    public String getStoreNumber() {
        return storeNumber;
    }

    public void setStoreNumber(String storeNumber) {
        this.storeNumber = storeNumber;
    }

    public boolean isPrePaymentResult() {
        return prePaymentResult;
    }

    public void setPrePaymentResult(boolean prePaymentResult) {
        this.prePaymentResult = prePaymentResult;
    }

    public boolean isFaultCancelPayment() {
        return faultCancelPayment;
    }

    public void setFaultCancelPayment(boolean faultCancelPayment) {
        this.faultCancelPayment = faultCancelPayment;
    }

    public SampleValueData getSampleValueData() {
        return sampleValueData;
    }

    public void setSampleValueData(SampleValueData sampleValueData) {
        this.sampleValueData = sampleValueData;
    }

    public StringBuilder getFaultMessage() {
        return faultMessage;
    }

    public void setFaultMessage(StringBuilder faultMessage) {
        this.faultMessage = faultMessage;
    }

    public boolean isAuthorizeResult() {
        return authorizeResult;
    }

    public void setAuthorizeResult(boolean authorizeResult) {
        this.authorizeResult = authorizeResult;
    }

    public int getRemaintime() {
        return remaintime;
    }

    public void setRemaintime(int remaintime) {
        this.remaintime = remaintime;
    }

    public String getParentIdTag() {
        return parentIdTag;
    }

    public void setParentIdTag(String parentIdTag) {
        this.parentIdTag = parentIdTag;
    }

    public String getParentIdTagStop() {
        return parentIdTagStop;
    }

    public void setParentIdTagStop(String parentIdTagStop) {
        this.parentIdTagStop = parentIdTagStop;
    }

    public String getIdTagStop() {
        return idTagStop;
    }

    public void setIdTagStop(String idTagStop) {
        this.idTagStop = idTagStop;
    }


    public int getResConnectorId() {
        return resConnectorId;
    }

    public void setResConnectorId(int resConnectorId) {
        this.resConnectorId = resConnectorId;
    }

    public String getResExpiryDate() {
        return resExpiryDate;
    }

    public void setResExpiryDate(String resExpiryDate) {
        this.resExpiryDate = resExpiryDate;
    }

    public String getResIdTag() {
        return resIdTag;
    }

    public void setResIdTag(String resIdTag) {
        this.resIdTag = resIdTag;
    }

    public String getResParentIdTag() {
        return resParentIdTag;
    }

    public void setResParentIdTag(String resParentIdTag) {
        this.resParentIdTag = resParentIdTag;
    }

    public String getResReservationId() {
        return resReservationId;
    }

    public void setResReservationId(String resReservationId) {
        this.resReservationId = resReservationId;
    }
    public ChargePointStatus getReservedStatus() {
        return reservedStatus;
    }

    public void setReservedStatus(ChargePointStatus reservedStatus) {
        this.reservedStatus = reservedStatus;
    }

    public void setRemainTimeStr(String remainTimeStr) {
        this.remainTimeStr = remainTimeStr;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getChangeMode() {
        return changeMode;
    }

    public void setChangeMode(String changeMode) {
        this.changeMode = changeMode;
    }

    public boolean isConnectUse() {
        return connectUse;
    }

    public void setConnectUse(boolean connectUse) {
        this.connectUse = connectUse;
    }

    public int getLimitSoc() {
        return limitSoc;
    }

    public void setLimitSoc(int limitSoc) {
        this.limitSoc = limitSoc;
    }

    public short getLimitPower() {
        return limitPower;
    }

    public void setLimitPower(short limitPower) {
        this.limitPower = limitPower;
    }

    public int getFullrechgsoc() {
        return fullrechgsoc;
    }

    public void setFullrechgsoc(int fullrechgsoc) {
        this.fullrechgsoc = fullrechgsoc;
    }

    public String getAuthNum() {
        return authNum;
    }

    public void setAuthNum(String authNum) {
        this.authNum = authNum;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public String getLimitApplyMID() {
        return limitApplyMID;
    }

    public void setLimitApplyMID(String limitApplyMID) {
        this.limitApplyMID = limitApplyMID;
    }

    public String getLimitApplyTid() {
        return limitApplyTid;
    }

    public void setLimitApplyTid(String limitApplyTid) {
        this.limitApplyTid = limitApplyTid;
    }

    public String getLimitApplySeq() {
        return limitApplySeq;
    }

    public void setLimitApplySeq(String limitApplySeq) {
        this.limitApplySeq = limitApplySeq;
    }

    public String getLimitApplyNumber() {
        return limitApplyNumber;
    }

    public void setLimitApplyNumber(String limitApplyNumber) {
        this.limitApplyNumber = limitApplyNumber;
    }

    public int getUsePoint() {
        return usePoint;
    }

    public void setUsePoint(int usePoint) {
        this.usePoint = usePoint;
    }

    public int getLimitApplyAmount() {
        return limitApplyAmount;
    }

    public void setLimitApplyAmount(int limitApplyAmount) {
        this.limitApplyAmount = limitApplyAmount;
    }

    public String getLimitApplyDt() {
        return limitApplyDt;
    }

    public void setLimitApplyDt(String limitApplyDt) {
        this.limitApplyDt = limitApplyDt;
    }

    public int getChargeIdx() {
        return chargeIdx;
    }

    public void setChargeIdx(int chargeIdx) {
        this.chargeIdx = chargeIdx;
    }

    public String getPartCancelTid() {
        return partCancelTid;
    }

    public void setPartCancelTid(String partCancelTid) {
        this.partCancelTid = partCancelTid;
    }

    public String getPartCancelNumber() {
        return partCancelNumber;
    }

    public void setPartCancelNumber(String partCancelNumber) {
        this.partCancelNumber = partCancelNumber;
    }

    public int getPartCancelAmount() {
        return partCancelAmount;
    }

    public void setPartCancelAmount(int partCancelAmount) {
        this.partCancelAmount = partCancelAmount;
    }

    public String getPartCancelDt() {
        return partCancelDt;
    }

    public void setPartCancelDt(String partCancelDt) {
        this.partCancelDt = partCancelDt;
    }

    public String getApprovalNo() {
        return approvalNo;
    }

    public void setApprovalNo(String approvalNo) {
        this.approvalNo = approvalNo;
    }

    public String getCreditNo() {
        return creditNo;
    }

    public void setCreditNo(String creditNo) {
        this.creditNo = creditNo;
    }
}
