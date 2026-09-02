package com.dongah.fastcharger.smartro;

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

public class ServiceProcessingActivity {
    private static final Logger logger = LoggerFactory.getLogger(ServiceProcessingActivity.class);

    private static final String SERVER_ACTION   = "smartro.vcat.action";
    private static final String SERVER_PACKAGE  = "service.vcat.smartro.com.vcat";
    private static final String VCAT_WAKEUP_URI = "smartroapp://vcatscheme?manage=awake";

    private final Context mContext;
    private SmartroVCatInterface mSmartroVCatInterface = null;
    private VCatListener mListener;
    private boolean mBound = false;

    public ServiceProcessingActivity(Context context) {
        this.mContext = context.getApplicationContext();
    }

    protected SmartroVCatInterface getVCatInterface() {
        return mSmartroVCatInterface;
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
     */
    public boolean bindVCATService() {
        if (mBound) return true;

        Intent intent = new Intent(SERVER_ACTION);
        intent.setPackage(SERVER_PACKAGE);

        boolean ok = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        if (!ok) {
            logger.warn("bindService failed. Try wakeUp() then bindVCATService() again.");
        }
        return ok;
    }

    /** V-CAT 서비스 언바인딩 */
    public void unbind() {
        if (mBound) {
            try {
                mContext.unbindService(mServiceConnection);
            } catch (IllegalArgumentException e) {
                logger.warn("unbindService: not registered.", e);
            }
            mBound = false;
            mSmartroVCatInterface = null;
        }
    }

    /**
     * V-CAT 앱 강제 실행 (문서 10번 항목)
     * bindVCATService() 실패·무응답 시 이 메서드 호출 후 bindVCATService() 재시도
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
            logger.error("wakeUp: V-CAT 강제 실행 실패", e);
        }
    }

    /**
     * V-CAT에 서비스 실행 요청
     * 결과는 listener.onEvent / onResult 로 전달됨
     */
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
     */
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
     */
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

    // -------------------------------------------------------------------------
    // 내부 - ServiceConnection / AIDL Callback
    // -------------------------------------------------------------------------

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
