package com.dongah.fastcharger.basefunction;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.pages.AdminPasswordFragment;
import com.dongah.fastcharger.pages.AuthSelectFragment;
import com.dongah.fastcharger.pages.ChargingFinishFragment;
import com.dongah.fastcharger.pages.ChargingFinishWaitFragment;
import com.dongah.fastcharger.pages.ChargingFragment;
import com.dongah.fastcharger.pages.ConfigSettingFragment;
import com.dongah.fastcharger.pages.ConnectionFailedFragment;
import com.dongah.fastcharger.pages.ConnectorCheckFragment;
import com.dongah.fastcharger.pages.ControlDebugFragment;
import com.dongah.fastcharger.pages.CreditCardFragment;
import com.dongah.fastcharger.pages.CreditCardWaitFragment;
import com.dongah.fastcharger.pages.DatabaseFragment;
import com.dongah.fastcharger.pages.EnvironmentFragment;
import com.dongah.fastcharger.pages.FaultFragment;
import com.dongah.fastcharger.pages.HeaderFragment;
import com.dongah.fastcharger.pages.InitFragment;
import com.dongah.fastcharger.pages.MemberCardFragment;
import com.dongah.fastcharger.pages.MemberCheckFailedFragment;
import com.dongah.fastcharger.pages.MemberCheckWaitFragment;
import com.dongah.fastcharger.pages.OperationStopFragment;
import com.dongah.fastcharger.pages.RemoteTestFragment;
import com.dongah.fastcharger.pages.ScreenSaverFragment;
import com.dongah.fastcharger.pages.WebSocketDebugFragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FragmentChange {

    public static final Logger logger = LoggerFactory.getLogger(FragmentChange.class);

    FragmentCurrent fragmentCurrent;

    public FragmentChange() {}

    public void onFragmentChange(int channel, UiSeq uiSeq, String sendText, String type) {
        Bundle bundle = new Bundle();
        bundle.putInt("CHANNEL", channel);
        ((MainActivity) MainActivity.mContext).setFragmentSeq(channel, uiSeq);
        int frameLayoutId = channel == 0 ? R.id.ch0 : R.id.ch1;
        // full = 1024*768, small = 512*692
        FragmentTransaction transaction = ((MainActivity) MainActivity.mContext).getSupportFragmentManager().beginTransaction();
        switch (uiSeq) {
            case INIT:
                try {
                    onFrameLayoutChange(false);
                    bundle.putInt("CHANNEL", channel == 0 ? 0 : 1);
                    InitFragment initFragment = new InitFragment();
                    initFragment.setArguments(bundle);
                    transaction.replace(frameLayoutId, initFragment, sendText);
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : INIT {}", e.getMessage());
                }
                break;
            case AUTH_SELECT:
                try {
                    onFrameLayoutChange(false);
                    AuthSelectFragment authFragment = new AuthSelectFragment();
                    authFragment.setArguments(bundle);
                    transaction.replace(frameLayoutId, authFragment, "AUTH_SELECT");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : AUTH_SELECT {}", e.getMessage());
                }
                break;
            case MEMBER_CHECK_WAIT:
                try {
                    onFrameLayoutChange(false);
                    MemberCheckWaitFragment memberCheckWaitFragment = new MemberCheckWaitFragment();
                    memberCheckWaitFragment.setArguments(bundle);
                    transaction.replace(frameLayoutId, memberCheckWaitFragment, "MEMBER_CHECK_WAIT");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : MEMBER_CHECK_WAIT {}", e.getMessage());
                }
                break;
            case CONNECTION_FAILED:
                try {
                    onFrameLayoutChange(false);
                    ConnectionFailedFragment connectionFailedFragment = new ConnectionFailedFragment();
                    connectionFailedFragment.setArguments(bundle);
                    transaction.replace(frameLayoutId, connectionFailedFragment, "CONNECTION_FAILED");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : CONNECTION_FAILED {}", e.getMessage());
                }
                break;
            case MEMBER_CARD:
                try {
                    onFrameLayoutChange(false);
                    MemberCardFragment memberCardFragment = new MemberCardFragment();
                    memberCardFragment.setArguments(bundle);
                    transaction.replace(frameLayoutId, memberCardFragment, "MEMBER_CARD");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : MEMBER_CARD {}", e.getMessage());
                }
                break;
            case MEMBER_CHECK_FAILED:
                try {
                    onFrameLayoutChange(false);
                    MemberCheckFailedFragment memberCheckFailedFragment = new MemberCheckFailedFragment();
                    memberCheckFailedFragment.setArguments(bundle);
                    transaction.replace(frameLayoutId, memberCheckFailedFragment, "MEMBER_CHECK_FAILED");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : MEMBER_CHECK_FAILED {}", e.getMessage());
                }
                break;
            case CREDIT_CARD:
                try {
                    onFrameLayoutChange(false);
                    CreditCardFragment creditCardFragment = new CreditCardFragment();
                    creditCardFragment.setArguments(bundle);
                    transaction.replace(frameLayoutId, creditCardFragment, "CREDIT_CARD");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : CREDIT_CARD {}", e.getMessage());
                }
                break;
            case CREDIT_CARD_WAIT:
                try {
                    onFrameLayoutChange(false);
                    CreditCardWaitFragment creditCardWaitFragment = new CreditCardWaitFragment();
                    creditCardWaitFragment.setArguments(bundle);
                    transaction.replace(frameLayoutId, creditCardWaitFragment, "CREDIT_CARD_WAIT");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : CREDIT_CARD_WAIT {}", e.getMessage());
                }
                break;
            case PLUG_CHECK:
                try {
                    onFrameLayoutChange(false);
                    ConnectorCheckFragment connectorCheckFragment = new ConnectorCheckFragment();
                    connectorCheckFragment.setArguments(bundle);
                    transaction.replace(frameLayoutId, connectorCheckFragment, "PLUG_CHECK");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : PLUG_CHECK {}", e.getMessage());
                }
                break;
            case CHARGING:
                try {
                    onFrameLayoutChange(false);
                    ChargingFragment chargingFragment = new ChargingFragment();
                    chargingFragment.setArguments(bundle);
                    transaction.replace(frameLayoutId, chargingFragment, "CHARGING");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : CHARGING {}", e.getMessage());
                }
                break;
            case FINISH_WAIT:
                try {
                    onFrameLayoutChange(false);
                    ChargingFinishWaitFragment chargingFinishWaitFragment = new ChargingFinishWaitFragment();
                    chargingFinishWaitFragment.setArguments(bundle);
                    transaction.replace(frameLayoutId, chargingFinishWaitFragment, "FINISH_WAIT");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : FINISH_WAIT {}", e.getMessage());
                }
                break;
            case FINISH:
                try {
                    onFrameLayoutChange(false);
                    ChargingFinishFragment chargingFinishFragment = new ChargingFinishFragment();
                    chargingFinishFragment.setArguments(bundle);
                    transaction.replace(frameLayoutId, chargingFinishFragment, "FINISH");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : FINISH {}", e.getMessage());
                }
                break;
            case FAULT:
                try {
                    onFrameLayoutChange(false);
                    FaultFragment faultFragment = new FaultFragment();
                    faultFragment.setArguments(bundle);
                    bundle.putString("param2", "FAULT_MESSAGE");
                    transaction.replace(frameLayoutId, faultFragment, "FAULT");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : FAULT {}", e.getMessage());
                }
                break;
            case REBOOTING:
                try {
                    onFrameLayoutChange(false);
                    FaultFragment faultFragment = new FaultFragment();
                    faultFragment.setArguments(bundle);
                    bundle.putString("param2", "REBOOTING");
                    bundle.putString("param3", type);
                    transaction.replace(frameLayoutId, faultFragment, "REBOOTING");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : REBOOTING {}", e.getMessage());
                }
                break;
            case OP_STOP:
                try {
                    onFrameLayoutChange(false);
                    OperationStopFragment operationStopFragment = new OperationStopFragment();
                    operationStopFragment.setArguments(bundle);
                    transaction.replace(frameLayoutId, operationStopFragment, "OP_STOP");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : OP_STOP {}", e.getMessage());
                }
                break;
            case ADMIN_PASS:
                try {
                    onFrameLayoutChange(true);
                    AdminPasswordFragment adminPasswordFragment = new AdminPasswordFragment();
                    adminPasswordFragment.setArguments(bundle);
                    transaction.replace(R.id.frameFull, adminPasswordFragment, "ADMIN_PASS");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : ADMIN_PASS {}", e.getMessage());
                }
                break;
            case ENVIRONMENT:
                try {
                    onFrameLayoutChange(true);
                    EnvironmentFragment environmentFragment = new EnvironmentFragment();
                    environmentFragment.setArguments(bundle);
                    transaction.replace(R.id.frameFull, environmentFragment, "ENVIRONMENT");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : ENVIRONMENT {}", e.getMessage());
                }
                break;
            case CONFIG_SETTING:
                try {
                    onFrameLayoutChange(true);
                    ConfigSettingFragment configSettingFragment = new ConfigSettingFragment();
                    configSettingFragment.setArguments(bundle);
                    transaction.replace(R.id.frameFull, configSettingFragment, "CONFIG_SETTING");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : CONFIG_SETTING {}", e.getMessage());
                }
                break;
            case CONTROL_BOARD_DEBUGGING:
                try {
                    onFrameLayoutChange(true);
                    ControlDebugFragment controlDebugFragment = new ControlDebugFragment();
                    controlDebugFragment.setArguments(bundle);
                    transaction.replace(R.id.frameFull, controlDebugFragment, "CONTROL_BOARD_DEBUGGING");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : CONTROL_BOARD_DEBUGGING {}", e.getMessage());
                }
                break;
            case WEB_SOCKET:
                try {
                    onFrameLayoutChange(true);
                    WebSocketDebugFragment webSocketDebugFragment = new WebSocketDebugFragment();
                    webSocketDebugFragment.setArguments(bundle);
                    transaction.replace(R.id.frameFull, webSocketDebugFragment, "WEB_SOCKET");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : WEB_SOCKET {}", e.getMessage());
                }
                break;
            case SCREEN_SAVER:
                try {
                    onFrameLayoutChange(true);
                    ScreenSaverFragment screenSaverFragment = new ScreenSaverFragment();
                    screenSaverFragment.setArguments(bundle);
                    transaction.replace(R.id.frameFull, screenSaverFragment, "SCREEN_SAVER");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : SCREEN_SAVER {}", e.getMessage());
                }
                break;
            case REMOTE_TEST:
                try {
                    onFrameLayoutChange(true);
                    RemoteTestFragment remoteTestFragment = new RemoteTestFragment();
                    remoteTestFragment.setArguments(bundle);
                    transaction.replace(R.id.frameFull, remoteTestFragment, "REMOTE_TEST");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : REMOTE_TEST {}", e.getMessage());
                }
                break;
            case DATABASE:
                try {
                    onFrameLayoutChange(true);
                    DatabaseFragment databaseFragment = new DatabaseFragment();
                    databaseFragment.setArguments(bundle);
                    transaction.replace(R.id.frameFull, databaseFragment, "DATABASE");
                    transaction.commit();
                } catch (Exception e) {
                    logger.error("onFragmentChange error : DATABASE {}", e.getMessage());
                }
                break;
            default:
                logger.error("onFragmentChange default error : {}", uiSeq);
                break;
        }
    }

    public void onFrameLayoutChange(boolean hidden) {
        //main activity layout fullScreen change
        MainActivity activity = (MainActivity) MainActivity.mContext;

        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            logger.error("onFrameLayoutChange skipped: activity is null or destroyed");
            return;
        }


        activity.runOnUiThread(() -> {
            try {
                FrameLayout frameLayout0 = activity.findViewById(R.id.ch0);
                FrameLayout frameLayout1 = activity.findViewById(R.id.ch1);
                FrameLayout fullScreen = activity.findViewById(R.id.frameFull);
                FrameLayout frameHeader = activity.findViewById(R.id.frameHeader);

                if (frameLayout0 == null || frameLayout1 == null || fullScreen == null || frameHeader == null) {
                    logger.error(
                            "onFrameLayoutChange view null: ch0={}, ch1={}, frameFull={}, frameHeader={}",
                            frameLayout0, frameLayout1, fullScreen, frameHeader
                    );
                    return;
                }

                if (hidden) {
                    fullScreen.setVisibility(View.VISIBLE);
                    frameLayout0.setVisibility(View.INVISIBLE);
                    frameLayout1.setVisibility(View.INVISIBLE);
                    frameHeader.setVisibility(View.INVISIBLE);
                } else {
                    onFrameLayoutRemove();

                    fullScreen.setVisibility(View.INVISIBLE);
                    frameLayout0.setVisibility(View.VISIBLE);
                    frameLayout1.setVisibility(View.VISIBLE);
                    frameHeader.setVisibility(View.VISIBLE);
                }

            } catch (Exception e) {
                logger.error("onFrameLayoutChange error", e);
            }
        });
    }

    public void onFragmentHeaderChange(int channel, String sendText) {
        try {
            Bundle bundle = new Bundle();
            bundle.putInt("CHANNEL", channel);
            int frameLayoutId = R.id.frameHeader;
            FragmentTransaction transaction = ((MainActivity) MainActivity.mContext).getSupportFragmentManager().beginTransaction();
            HeaderFragment headerFragment = new HeaderFragment();
            transaction.replace(frameLayoutId, headerFragment, sendText);
            headerFragment.setArguments(bundle);
            transaction.commit();
        } catch (Exception e) {
            logger.error("onFragmentHeaderChange error : {}", e.getMessage());
        }
    }

    public void onFrameLayoutRemove(){
        try {
            fragmentCurrent = new FragmentCurrent();
            FragmentTransaction transaction = ((MainActivity) MainActivity.mContext).getSupportFragmentManager().beginTransaction();
            Fragment fragment = fragmentCurrent.getCurrentFragment();
            if (fragment != null) {
                transaction.remove(fragment); // 제거
                transaction.commit(); // UI 반영
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    public boolean onFragmentScreenSaverChange() {
        try {
            UiSeq ui0 = ((MainActivity) MainActivity.mContext).getClassUiProcess(0) != null
                    ? ((MainActivity) MainActivity.mContext).getClassUiProcess(0).getUiSeq()
                    : null;
            UiSeq ui1 = ((MainActivity) MainActivity.mContext).getClassUiProcess(1) != null
                    ? ((MainActivity) MainActivity.mContext).getClassUiProcess(1).getUiSeq()
                    : null;

            boolean chkUiSeq = ui0 == UiSeq.INIT && ui1 == UiSeq.INIT;

            if (chkUiSeq) {
                FragmentTransaction transaction = ((MainActivity) MainActivity.mContext).getSupportFragmentManager().beginTransaction();
                onFrameLayoutChange(true);
                ScreenSaverFragment screenSaverFragment = new ScreenSaverFragment();
                transaction.replace(R.id.frameFull, screenSaverFragment, "SCREEN_SAVER");
                transaction.commit();
                return true;
            }
        } catch (Exception e) {
            Log.e("FragmentChange", "onFragmentScreenSaverChange error", e);
            logger.error("FragmentChange onFragmentScreenSaverChange error : {}", e.getMessage());
        }
        return false;
    }
}
