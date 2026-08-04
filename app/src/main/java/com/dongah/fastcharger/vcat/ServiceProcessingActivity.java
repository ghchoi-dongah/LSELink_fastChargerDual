package com.dongah.fastcharger.vcat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;


import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import service.vcat.smartro.com.vcat.SmartroVCatCallback;
import service.vcat.smartro.com.vcat.SmartroVCatInterface;

/**
 * 스마트로 V-CAT 서비스 연동 매니저
 *
 * 사용 순서:
 *   1) VCatManager vcat = new VCatManager(context);
 *   2) vcat.setListener(listener);
 *   3) vcat.bind();                 // onCreate
 *   4) vcat.executeService(json);   // 결제/기능 호출
 *   5) vcat.unbind();               // onDestroy
 *
 *   1) Application.onCreate() 에서 VCatManager.getInstance().init(this)
 *   2) VCatManager.getInstance().bind()  (앱 시작 시 1회)
 *   3) 결제 필요 시 execute(json, listener)
 *   4) Application.onTerminate() 또는 앱 종료 시 unbind()
 */
public class VCatManager {
    private static final Logger logger = LoggerFactory.getLogger(VCatManager.class);

    private static final String VCAT_ACTION  = "smartro.vcat.action";
    private static final String VCAT_PACKAGE = "service.vcat.smartro.com.vcat";
    private static final String VCAT_WAKEUP_URI = "smartroapp://vcatscheme?manage=awake";


    private final Context mContext;
    private SmartroVCatInterface mSmartroVCatInterface  = null;
    private VCatListener mListener;
    private boolean mBound = false;


    public VCatManager(Context context) {
        // ApplicationContext 사용 - Activity 누수 방지
        this.mContext = context.getApplicationContext();
    }

    public void setListener(VCatListener listener) {
        this.mListener = listener;
    }

    public boolean isBound() {
        return mBound && mSmartroVCatInterface != null;
    }

    /**
     * V-CAT 서비스 바인딩
     * @return bindService 호출 성공 여부 (실제 연결은 onServiceConnected 에서 확인)
     * */
    public boolean bind() {
        if (mBound) return true;

        Intent intent = new Intent(VCAT_ACTION);
        intent.setPackage(VCAT_PACKAGE);
        intent.putExtra("package", mContext.getPackageName());

        boolean ok = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        if (!ok) {
            // 일부 장비에서 V-CAT이 비활성 상태이면 bindService가 실패하거나 콜백이 오지 않음.
            // 문서 10장: 강제 실행 후 재바인딩.
            logger.warn("bindService failed. Try wakeUp() then bind() again.");
        }
        return ok;
    }

    /** V-CAT 서비스 언바인딩 */
    public void unbind() {
        if (mBound) {
            try {
                mContext.unbindService(mServiceConnection);
            } catch (IllegalArgumentException e) {
                logger.warn("unbindService: not registered. ", e);
            }
            mBound = false;
            mSmartroVCatInterface = null;
        }
    }

    /**
     * V-CAT 강제 실행 (문서 10번 항목)
     * 일부 장비에서 bindService 실패/무응답 시, 이 메서드 호출 후 다시 bind() 시도
     *
     * 주의: Activity Context 가 필요한 startActivity 이므로
     *       Activity 에서 직접 호출하거나, FLAG_ACTIVITY_NEW_TASK 사용
     */
    public void wakeUp() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse(VCAT_WAKEUP_URI));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            mContext.startActivity(intent);
        } catch (Exception e) {
            logger.error("wakeUp: V-CAT 강제 실행 실패 ", e);
        }
    }

    /**
     * V-CAT에 서비스 실행 요청
     * 결과는 listener.onEvent / onResult로 전달됨
     * */
    public boolean executeService(String requestJson) {
        if (!isBound()) {
            logger.error("executeService: not bound.");
            return false;
        }

        try {
            mSmartroVCatInterface.executeService(requestJson, mCallback);
            return true;
        } catch (RemoteException e) {
            logger.error("executeService error", e);
            return false;
        }
    }

    /**
     * prompt 이벤트(서명, pay-type 선택, DCC 통화 선택 등)에 대한 추가 데이터 전달
     * */
    public boolean postExtraData(@NonNull String extraJson) {
        if (!isBound()) {
            logger.error("postExtraData: not bound.");
            return false;
        }
        try {
            mSmartroVCatInterface.postExtraData(extraJson);
            return true;
        } catch (RemoteException e) {
            logger.error("postExtraData error", e);
            return false;
        }
    }

    /**
     * 현재 진행 중인 세션 강제 종료
     * 주의: 호출 후 onServiceResult 응답을 받은 뒤에 다음 거래를 실행해야 함
     * ("인스턴스가 사용중입니다" 오류 방지)
     * */
    public boolean cancelService() {
        if (!isBound()) return false;
        try {
            mSmartroVCatInterface.cancelService();
            return true;
        } catch (RemoteException e) {
            logger.error("cancelService error", e);
            return false;
        }
    }


    // ---------------------------------------------------------------------
    // 내부 - ServiceConnection / AIDL Callback
    // ---------------------------------------------------------------------
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSmartroVCatInterface = SmartroVCatInterface.Stub.asInterface(service);
            mBound = true;
            logger.info("V-CAT service connected.");
            if (mListener != null) mListener.onServiceConnectionChanged(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSmartroVCatInterface = null;
            mBound = false;
            logger.info("V-CAT service disconnected.");
            if (mListener != null) mListener.onServiceConnectionChanged(false);
        }
    };

    // ------------------------------------------------------------------
    // Callback stub → UI 스레드 dispatch
    // ------------------------------------------------------------------
    private final SmartroVCatCallback.Stub mCallback = new SmartroVCatCallback.Stub() {
        @Override
        public void onServiceEvent(String strEventJSON) throws RemoteException {
            logger.info("onServiceEvent: {}", strEventJSON);
            if (mListener != null) mListener.onEvent(strEventJSON);
        }

        @Override
        public void onServiceResult(String strResultJSON) throws RemoteException {
            logger.info("onServiceResult: {}", strResultJSON);
            if (mListener != null) mListener.onResult(strResultJSON);
        }
    };
}
