package com.dongah.fastcharger.pages;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.ClassUiProcess;
import com.dongah.fastcharger.basefunction.FragmentChange;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.controlboard.RxData;
import com.dongah.fastcharger.controlboard.TxData;
import com.dongah.fastcharger.smartro.VCatPaymentManager;
import com.dongah.fastcharger.utils.BitUtilities;
import com.dongah.fastcharger.utils.SharedModel;
import com.dongah.fastcharger.websocket.ocpp.core.ChargePointErrorCode;
import com.dongah.fastcharger.websocket.ocpp.core.ChargePointStatus;
import com.dongah.fastcharger.websocket.ocpp.core.Reason;
import com.dongah.fastcharger.websocket.socket.SocketReceiveMessage;
import com.dongah.fastcharger.websocket.socket.SocketState;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.AuthorizeReq;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.StatusNotificationReq;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConnectorCheckFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 *
 * [변경] ServiceProcessingFragment → Fragment
 *   V-CAT 바인딩을 VCatPaymentManager 싱글톤으로 위임
 *   → requestNoCardCancel() / handleCancelResult() 제거
 *   → VCatPaymentManager.requestChargeFinishCancel(usedPayment=0) 으로 대체
 *
 * 타임아웃 흐름:
 *   isPrePaymentResult() == true  → VCatPaymentManager 무카드 전체취소 → 홈 이동
 *   isPrePaymentResult() == false → 바로 홈 이동
 */
public class ConnectorCheckFragment extends Fragment {
    private static final Logger logger = LoggerFactory.getLogger(ConnectorCheckFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;


    int cnt = 0;
    TextView textViewConnectorCheckMessage, textViewFailed, textViewConnector;
    ImageView imageViewLoading, imageViewConnectionFailed;
    AnimationDrawable animationDrawable;
    ObjectAnimator fadeAnimator;
    RxData rxData;
    TxData txData;
    Handler countHandler;
    Runnable countRunnable;
    SharedModel sharedModel;
    String[] requestStrings = new String[1];
    MainActivity activity;
    ClassUiProcess classUiProcess;
    ChargerConfiguration chargerConfiguration;
    ChargingCurrentData chargingCurrentData;
    FragmentChange fragmentChange;
    private boolean cancelInProgress = false;

    public ConnectorCheckFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ConnectorCheckFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ConnectorCheckFragment newInstance(String param1, String param2) {
        ConnectorCheckFragment fragment = new ConnectorCheckFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
            mChannel = getArguments().getInt(CHANNEL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connector_check, container, false);
        textViewConnectorCheckMessage = view.findViewById(R.id.textViewConnectorCheckMessage);
        imageViewLoading = view.findViewById(R.id.imageViewLoading);
        imageViewLoading.setBackgroundResource(R.drawable.ani_loading);
        animationDrawable = (AnimationDrawable) imageViewLoading.getBackground();
        imageViewConnectionFailed = view.findViewById(R.id.imageViewConnectionFailed);
        textViewFailed = view.findViewById(R.id.textViewFailed);
        textViewConnector = view.findViewById(R.id.textViewConnector);

        // textViewFailed animation
        fadeAnimator = ObjectAnimator.ofFloat(textViewFailed, "alpha", 1f, 0.2f);
        fadeAnimator.setDuration(1000);
        fadeAnimator.setRepeatCount(ValueAnimator.INFINITE);
        fadeAnimator.setRepeatMode(ValueAnimator.REVERSE);
        fadeAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        activity = ((MainActivity) MainActivity.mContext);
        classUiProcess = activity.getClassUiProcess(mChannel);
        chargerConfiguration = activity.getChargerConfiguration();
        chargingCurrentData = activity.getChargingCurrentData(mChannel);
        fragmentChange = activity.getFragmentChange();
        rxData = activity.getControlBoard().getRxData(mChannel);
        txData = activity.getControlBoard().getTxData(mChannel);

        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            sharedModel = new ViewModelProvider(requireActivity()).get(SharedModel.class);
            requestStrings[0] = String.valueOf(mChannel);
            sharedModel.setMutableLiveData(requestStrings);

            cnt = 0;
            animationDrawable.start();
            startTimeoutCounter();
        } catch (Exception e) {
            logger.error("onViewCreated error : {}", e.getMessage());
        }
    }

    // connection time out
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startTimeoutCounter() {
        try {
            countHandler = new Handler();
            countRunnable = new Runnable() {
                @Override
                public void run() {
                    // [추가] Fragment 분리 상태 가드
                    if (!isAdded() || getActivity() == null) {
                        Log.w("PLUG", "guard hit: isAdded=" + isAdded()
                                + " activity=" + getActivity() + " cnt=" + cnt);
                        stopTimeoutCounter();
                        return;
                    }

                    cnt++;

                    // connecting wait
                    if (rxData.isCsPilot()) {
                        if (textViewConnectorCheckMessage.getTag() == null || !(boolean) textViewConnectorCheckMessage.getTag()) {
                            textViewConnectorCheckMessage.setText(R.string.EVCheckMessage);
                            textViewConnectorCheckMessage.setTag(true);
                        }
                    }

                    // timeout
                    if (cnt >= GlobalVariables.getConnectionTimeOut()) {
                        // 충전기 종료
                        stopTimeoutCounter();
                        onTimeout();
                    } else {
                        countHandler.postDelayed(countRunnable, 1000);
                    }
                }
            };
            countHandler.postDelayed(countRunnable, 1000);
        } catch (Exception e) {
            logger.error("startTimeoutCounter error : {}", e.getMessage(), e);
        }
    }

    /**
     * 타임아웃 발생 시 처리
     *
     * 비회원 선결제(isPrePaymentResult) 완료 상태 → 무카드 전체취소
     * 그 외 → 바로 홈 이동
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void onTimeout() {
        try {
            // ── 비회원 선결제 완료 → 무카드 전체취소 ─────────────
            if (chargingCurrentData.isPrePaymentResult()) {
                logger.info("비회원 선결제 타임아웃 → 무카드 전체취소 요청");
                requestNoCardCancelViaSingleton();
            } else {
                // 선결제 없음 → 바로 홈
                goHome();
            }
        } catch (Exception e) {
            logger.error("onTimeout error : {}", e.getMessage(), e);
        }
    }

    // ── 무카드 전체취소 (VCatPaymentManager 위임) ─────────

    /**
     * VCatPaymentManager.requestChargeFinishCancel(usedPayment=0) 호출
     *
     * usedPayment=0 → 내부에서 deal="cancellation" (전체취소) 로 처리
     *
     * [제거된 코드]
     *   - requestNoCardCancel()   : V-CAT JSON 직접 빌드 + getVCatInterface() 직접 호출
     *   - handleCancelResult()    : 결과 파싱 및 상태 처리
     *   - isServiceConnected()    : ServiceProcessingFragment 전용 메서드
     *   - cancelInProgress 일부   : VCatPaymentManager 내부에서 관리
     */
    private void requestNoCardCancelViaSingleton() {
        if (cancelInProgress) {
            logger.warn("requestNoCardCancelViaSingleton: 이미 취소 진행 중");
            return;
        }

        // VCatPaymentManager 연결 여부 확인
        if (!VCatPaymentManager.getInstance().isConnected()) {
            logger.error("requestNoCardCancelViaSingleton: V-CAT 미연결 → 홈 이동");
            showToast("결제 취소 서비스에 연결할 수 없습니다. 고객센터에 문의해주세요.");
            goHome();
            return;
        }

        cancelInProgress = true;


        // usedPayment = 0 → requestChargeFinishCancel 내부에서 "cancellation" 처리
        VCatPaymentManager.getInstance().requestChargeFinishCancel(
                0,
                chargingCurrentData,
                chargerConfiguration,
                chargingCurrentData.getConnectorId(),
                new VCatPaymentManager.PaymentCallback() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onSuccess(JSONObject result) {
                        cancelInProgress = false;
                        logger.info("무카드 전체취소 성공 - 취소승인번호: {}",
                                result.optString("approval-no", ""));

                        chargingCurrentData.setTradeCode(result.optString("response-code", ""));    // 결제 승인코드, "00" 값이 아니면 거절
                        chargingCurrentData.setTradeMethod(result.optString("display-msg", ""));    // 화면 메시지
                        chargingCurrentData.setPrePaymentResult(false);

                        // f2 전문 전송: 전체취소 결과 CSMS 전달 (성공 - 전체 필드)
//                        sendPartCancelResultToServer(chargingCurrentData.getConnectorId(), true);

                        showToast("결제가 취소되었습니다.");
                        goHome();
                    }

                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onFailure(String errorMessage) {
                        cancelInProgress = false;
                        logger.error("무카드 전체취소 실패: {}", errorMessage);

                        // f2 전문 전송: 전체취소 결과 CSMS 전달 (실패 - transaction_id 만)
//                        sendPartCancelResultToServer(chargingCurrentData.getConnectorId(), false);

                        showToast("결제 취소 실패: 고객운영 센터로 연락하세요!");
                        goHome();
                    }
                }
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void stopTimeoutCounter() {
        if (countHandler != null && countRunnable != null) {
            countHandler.removeCallbacks(countRunnable);
        }

        txData.setStart(false);
        txData.setStop(true);

        // preparing
        if (Objects.equals(chargingCurrentData.getChargePointStatus(), ChargePointStatus.Preparing) &&
                Objects.equals(chargerConfiguration.getOpMode(), 1) &&
                !((MainActivity) MainActivity.mContext).getControlBoard().getRxData(mChannel).isCsPilot()) {
            chargingCurrentData.setChargePointStatus(ChargePointStatus.Available);
            chargingCurrentData.setChargePointErrorCode(ChargePointErrorCode.NoError);

            // StatusNotification
            StatusNotificationReq statusNotificationReq = new StatusNotificationReq(chargingCurrentData.getConnectorId());
            statusNotificationReq.sendStatusNotification();
        }

        // 통신 실패
        classUiProcess.setUiSeq(UiSeq.CONNECTION_FAILED);
        fragmentChange.onFragmentChange(mChannel, UiSeq.CONNECTION_FAILED, "CONNECTION_FAILED", null);
    }

    private boolean isControlBoardAvailable() {
        return activity != null && activity.getControlBoard() != null;
    }

    private void goHome() {
        if (!isAdded() || getActivity() == null) return;
        activity.getClassUiProcess(mChannel).onHome();
    }

    private void showToast(String message) {
        if (!isAdded() || getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            try {
                android.widget.Toast.makeText(requireContext(), message,
                        android.widget.Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                logger.error("showToast error: {}", e.getMessage());
            }
        });
    }

    @Override
    public void onDestroyView() {
        try {
            if (fadeAnimator != null) {
                fadeAnimator.cancel();
                fadeAnimator = null;
            }

            if (animationDrawable != null) {
                animationDrawable.stop();
            }

            if (imageViewLoading != null) {
                Drawable bg = imageViewLoading.getBackground();
                if (bg instanceof AnimationDrawable) {
                    ((AnimationDrawable) bg).stop();
                }
                imageViewLoading.setBackground(null);
            }

            if (countHandler != null) {
                countHandler.removeCallbacks(countRunnable);
                countHandler.removeCallbacksAndMessages(null);
                countHandler.removeMessages(0);
            }
            countRunnable = null;

        } catch (Exception e) {
            logger.error("onDestroyView error : {}", e.getMessage());
        }
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}