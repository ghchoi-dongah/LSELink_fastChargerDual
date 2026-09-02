package com.dongah.fastcharger.smartro;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import service.vcat.smartro.com.vcat.SmartroVCatInterface;

public class ServiceProcessingFragment extends Fragment {
    private static final Logger logger = LoggerFactory.getLogger(ServiceProcessingFragment.class);
    private static final String SERVER_ACTION   = "smartro.vcat.action";
    private static final String SERVER_PACKAGE  = "service.vcat.smartro.com.vcat";

    // 안전한 서비스 바인딩 상태 관리를 위해 volatile 키워드 사용
    private volatile SmartroVCatInterface mSmartroVCatInterface = null;
    private boolean isServiceBound = false;

    /** 서비스 연결/해제 콜백 */
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSmartroVCatInterface = SmartroVCatInterface.Stub.asInterface(service);
            isServiceBound = true;
            try {
                connectedWithService();
            } catch (Exception e) {
                logger.error("Error in connectedWithService: {}", e.getMessage(), e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSmartroVCatInterface = null;
            isServiceBound = false;
            disconnectedWithService();
        }
    };

    // 기본 생성자 필수 (Fragment 재생성 대비)
    public ServiceProcessingFragment() {
        // Required empty public constructor
    }

    private void disconnectedWithService() {
        logger.warn("SMARTRO VCAT Service Disconnected.");
    }

    protected void connectedWithService() {
        logger.info("SMARTRO VCAT Service Connected Successfully.");
    }

    protected SmartroVCatInterface getVCatInterface() {
        return mSmartroVCatInterface;
    }

    protected boolean isServiceConnected() {
        return isServiceBound && mSmartroVCatInterface != null;
    }

    protected void writeLog(String strText) {
        logger.info(strText);
    }

    protected void showToast(final String strMessage) {
        // Fragment가 Activity에 정상적으로 붙어있을 때만 UI 스레드에서 토스트 실행
        if (isAdded() && getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    Toast.makeText(requireContext(), strMessage, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    logger.error("Toast error: ", e);
                }
            });
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindSmartroService();
    }

    @Override
    public void onDestroy() {
        unbindSmartroService();
        super.onDestroy();
    }

    private void bindSmartroService() {
        try {
            awakeVCat();
            Intent intent = new Intent(SERVER_ACTION);
            intent.setPackage(SERVER_PACKAGE);
            // MainActivity.mContext 대신 requireContext()를 사용하여 메모리 누수 방지
            boolean success = requireContext().bindService(
                    intent, serviceConnection, Context.BIND_AUTO_CREATE);
            if (!success) {
                logger.error("SMARTRO VCAT Service Bind Failed.");
            }
        } catch (Exception e) {
            logger.error("Exception during bindService: ", e);
        }
    }

    private void unbindSmartroService() {
        if (isServiceBound) {
            try {
                requireContext().unbindService(serviceConnection);
            } catch (Exception e) {
                logger.error("Exception during unbindService: ", e);
            } finally {
                mSmartroVCatInterface = null;
                isServiceBound = false;
            }
        }
    }

    private void awakeVCat() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("smartroapp://vcatscheme?manage=awake"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            requireContext().startActivity(intent);
        } catch (Exception e) {
            // V-CAT 미설치 또는 URI 미지원 기기 → 무시하고 계속 진행
            logger.warn("awakeVCat skipped: {}", e.getMessage());
        }
    }
}
