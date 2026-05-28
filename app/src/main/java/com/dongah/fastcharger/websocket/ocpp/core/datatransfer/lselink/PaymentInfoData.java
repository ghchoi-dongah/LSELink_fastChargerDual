package com.dongah.fastcharger.websocket.ocpp.core.datatransfer.lselink;

import com.dongah.fastcharger.websocket.ocpp.common.model.Validatable;

public class PaymentInfoData implements Validatable {
    public String tid;                      // 결제승인관리번호
    public String trantype;                 // 요청코드(승인: 0210, 부분취소: 0431, 전체취소: 0430)
    public String errcode;                  // 정상승인: 0000, 실패 시 PG에서 받은 코드 입력
    public String cardno;                   // 카드 번호
    public int halbu;                       // 할부개월
    public int tamt;                        // 결제금액
    public String trandate;                 // 승인일자
    public String trantime;                 // 승인시간
    public String authno;                   // 승인번호
    public String merno;                    // 가맹점번호
    public String tran_serial;              // 가맹점일련번호
    public String stlinst;                  // 발급사명
    public String reqinst;                  // 매입사명
    public String signpath;                 // 서명
    public String msg1;                     // 승인 메시지
    public String msg2;
    public String msg3;
    public String msg4;                     // 실패 내역 입력

    public PaymentInfoData() {}

    public String getTid() {
        return tid;
    }

    public void setTid(String tid) {
        this.tid = tid;
    }

    public String getTrantype() {
        return trantype;
    }

    public void setTrantype(String trantype) {
        this.trantype = trantype;
    }

    public String getErrcode() {
        return errcode;
    }

    public void setErrcode(String errcode) {
        this.errcode = errcode;
    }

    public String getCardno() {
        return cardno;
    }

    public void setCardno(String cardno) {
        this.cardno = cardno;
    }

    public int getHalbu() {
        return halbu;
    }

    public void setHalbu(int halbu) {
        this.halbu = halbu;
    }

    public int getTamt() {
        return tamt;
    }

    public void setTamt(int tamt) {
        this.tamt = tamt;
    }

    public String getTrandate() {
        return trandate;
    }

    public void setTrandate(String trandate) {
        this.trandate = trandate;
    }

    public String getTrantime() {
        return trantime;
    }

    public void setTrantime(String trantime) {
        this.trantime = trantime;
    }

    public String getAuthno() {
        return authno;
    }

    public void setAuthno(String authno) {
        this.authno = authno;
    }

    public String getMerno() {
        return merno;
    }

    public void setMerno(String merno) {
        this.merno = merno;
    }

    public String getTran_serial() {
        return tran_serial;
    }

    public void setTran_serial(String tran_serial) {
        this.tran_serial = tran_serial;
    }

    public String getStlinst() {
        return stlinst;
    }

    public void setStlinst(String stlinst) {
        this.stlinst = stlinst;
    }

    public String getReqinst() {
        return reqinst;
    }

    public void setReqinst(String reqinst) {
        this.reqinst = reqinst;
    }

    public String getSignpath() {
        return signpath;
    }

    public void setSignpath(String signpath) {
        this.signpath = signpath;
    }

    public String getMsg1() {
        return msg1;
    }

    public void setMsg1(String msg1) {
        this.msg1 = msg1;
    }

    public String getMsg2() {
        return msg2;
    }

    public void setMsg2(String msg2) {
        this.msg2 = msg2;
    }

    public String getMsg3() {
        return msg3;
    }

    public void setMsg3(String msg3) {
        this.msg3 = msg3;
    }

    public String getMsg4() {
        return msg4;
    }

    public void setMsg4(String msg4) {
        this.msg4= msg4;
    }


    @Override
    public boolean validate() {
        return true;
    }
}
