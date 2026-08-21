package com.dongah.fastcharger.pages;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import android.os.Handler;
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
import com.dongah.fastcharger.websocket.ocpp.core.ChargePointStatus;
import com.dongah.fastcharger.websocket.ocpp.core.Reason;
import com.dongah.fastcharger.websocket.socket.SocketReceiveMessage;
import com.dongah.fastcharger.websocket.socket.SocketState;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.AuthorizeReq;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.StatusNotificationReq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MemberCheckWaitFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MemberCheckWaitFragment extends Fragment {

    private static final Logger logger = LoggerFactory.getLogger(MemberCheckWaitFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    int TIME_MAX = 20;
    int cnt = 0;
    boolean isFlag = false;
    TextView textViewMemberWaitMessage, textViewFailed, textViewConnectorRetryMessage, textViewMemberRegistMessage;
    ImageView imageViewLoading, imageViewMemberFailed;
    AnimationDrawable animationDrawable;
    ObjectAnimator fadeAnimator;

    MediaPlayer mediaPlayer;
    RxData rxData;

    MainActivity activity;
    ClassUiProcess classUiProcess;
    ChargingCurrentData chargingCurrentData;
    ChargerConfiguration chargerConfiguration;
    FragmentChange fragmentChange;

    Handler countHandler;
    Runnable countRunnable;

    public MemberCheckWaitFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MemberCheckWaitFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MemberCheckWaitFragment newInstance(String param1, String param2) {
        MemberCheckWaitFragment fragment = new MemberCheckWaitFragment();
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
        View view = inflater.inflate(R.layout.fragment_member_check_wait, container, false);
        imageViewLoading = view.findViewById(R.id.imageViewLoading);
        imageViewLoading.setBackgroundResource(R.drawable.ani_loading);
        animationDrawable = (AnimationDrawable) imageViewLoading.getBackground();
        textViewMemberWaitMessage = view.findViewById(R.id.textViewMemberWaitMessage);
        imageViewMemberFailed = view.findViewById(R.id.imageViewMemberFailed);
        textViewFailed = view.findViewById(R.id.textViewFailed);
        textViewConnectorRetryMessage = view.findViewById(R.id.textViewConnectorRetryMessage);
        textViewMemberRegistMessage = view.findViewById(R.id.textViewMemberRegistMessage);

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
        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            rxData = activity.getControlBoard().getRxData(mChannel);
            isFlag = false;
            animationDrawable.start();
            mediaPlayer();   // media player

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    countHandler = new Handler();
                    countRunnable = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                cnt++;
                                if (cnt > TIME_MAX) {
                                    countHandler.removeCallbacks(countRunnable);

                                    if (Objects.equals(classUiProcess.getUiSeq(), UiSeq.CHARGING)) {
                                        fragmentChange.onFragmentChange(mChannel, UiSeq.CHARGING, "CHARGING", null);
                                    } else {
                                        // 회원 인증 실패
                                        classUiProcess.setUiSeq(UiSeq.MEMBER_CHECK_FAILED);
                                        fragmentChange.onFragmentChange(mChannel, UiSeq.MEMBER_CHECK_FAILED, "MEMBER_CHECK_FAILED", null);
                                    }
                                } else {
                                    countHandler.postDelayed(countRunnable, 1000);
                                }
                            } catch (Exception e) {
                                logger.error("onViewCreated run error : {}", e.getMessage());
                            }
                        }
                    };
                    countHandler.postDelayed(countRunnable, 1000);
                }
            });

            // 나중에 부활 예정
            String[] idTagInfo;
            UiSeq uiSeq = classUiProcess.getUiSeq();
            SocketReceiveMessage socketReceiveMessage = activity.getSocketReceiveMessage();

            // reservation check
            if (chargingCurrentData.getReservedStatus() == ChargePointStatus.Reserved) {
                if (!Objects.equals(chargingCurrentData.getResIdTag(), chargingCurrentData.getIdTag())) {
                    // resIdTag ≠ idTag
                    if (Objects.equals(uiSeq, UiSeq.CHARGING)) {
                        // 현재 UI가 CHARING
                        // 충전 중 다른 카드 태그 → resIdTag로 재인증(Authorize 요청)
                        // 충전 중인데 다른 카드를 태그 → 태그한 그 카드로 인증 시도
                        AuthorizeReq authorizeReq = new AuthorizeReq(chargingCurrentData.getConnectorId());
                        authorizeReq.sendAuthorize(chargingCurrentData.getResIdTag());
                    } else {
                        if (!Objects.equals(chargingCurrentData.getResParentIdTag(), "")) {
                            // Charging 아닌 상태 && resParentIdTag가 있음 → idTag로 Authorize 요청(그룹 카드 하위 카드 → 원래 카드로 인증)
                            // 충전 시작 전인데 카드 ID가 다름 + 상위 그룹 카드가 있음 → 원래 카드로 다시 인증 시도
                            AuthorizeReq authorizeReq = new AuthorizeReq(chargingCurrentData.getConnectorId());
                            authorizeReq.sendAuthorize(chargingCurrentData.getIdTag());
                        } else {
                            // Charging 아닌 상태 && resParentIdTag가 없음 → 인증 불가로 HOME 이동
                            // 충전 시작 전인데 카드 ID가 다름 + 상위 카드도 없음 → 인증 불가, HOME 이동
                            Toast.makeText(getActivity(), "예약한 회원번호가 틀립니다.", Toast.LENGTH_SHORT).show();
                            activity.getClassUiProcess(mChannel).onHome();
                        }
                    }
                    return;
                } else {
                    // Authorization, resIdTag == idTag
                    if (Objects.equals(uiSeq, UiSeq.CHARGING)) {
                        // 현재 UI가 CHARGING
                        idTagInfo = socketReceiveMessage.getLocalAuthorizationListStrings(chargingCurrentData.getIdTagStop());
                        if (Objects.equals(chargingCurrentData.getParentIdTag(), idTagInfo[1]) ||
                                Objects.equals(chargingCurrentData.getIdTag(), chargingCurrentData.getIdTagStop())) {
                            // 같은 카드 or 상위 그룹 카드로 태그 → 충전 종료
                            chargingCurrentData.setUserStop(true);
                        } else {
                            // 다른 카드로 태그 → 충전 화면 유지
                            // 식별자가 상이할 경우 Authorize 전송
                            AuthorizeReq authorizeReq = new AuthorizeReq(chargingCurrentData.getConnectorId());
                            authorizeReq.sendAuthorize(chargingCurrentData.getIdTagStop());
                            fragmentChange.onFragmentChange(mChannel,UiSeq.CHARGING, "CHARGING", null);
                        }
                    } else {
                        // 충전 시작 전
                        chargingCurrentData.setIdTag(chargingCurrentData.getResIdTag()); // idTag를 resIdTag로 업데이트
                        AuthorizeReq authorizeReq = new AuthorizeReq(chargingCurrentData.getConnectorId());
                        authorizeReq.sendAuthorize(chargingCurrentData.getIdTag());
                    }
                    return;
                }
            }

            // isLocalPreAuthorize == true : local authorization list 에서 사용자 인증
            // isLocalPreAuthorize: 사전 로컬 인증 모드
            if (GlobalVariables.isLocalPreAuthorize()) {
                // local authorization enabled --> local 인증
                idTagInfo = socketReceiveMessage.getLocalAuthorizationListStrings(uiSeq == UiSeq.CHARGING ?
                        chargingCurrentData.getIdTagStop() : chargingCurrentData.getIdTag());
                if (Objects.equals(UiSeq.CHARGING, uiSeq)) {
                    // 충전 중 상태
                    if (Objects.equals(chargingCurrentData.getParentIdTag(), idTagInfo[1]) ||
                            Objects.equals(chargingCurrentData.getIdTag(), chargingCurrentData.getIdTagStop())) {
                        // 같은 카드 or 그룹 카드 → 충전 종료
                        chargingCurrentData.setUserStop(true);
                    } else  {
                        // 다른 카드 → 충전 화면 유지
                        // 식별자가 상이할 경우 Authorize 전송
                        AuthorizeReq authorizeReq = new AuthorizeReq(chargingCurrentData.getConnectorId());
                        authorizeReq.sendAuthorize(chargingCurrentData.getIdTagStop());
                        activity.getFragmentChange().onFragmentChange(mChannel, UiSeq.CHARGING, "CHARGING", null);
                    }
                } else {
                    // 충전 전
                    if (!Objects.equals(chargingCurrentData.getChargePointStatus(), ChargePointStatus.Preparing) &&
                            Objects.equals(chargerConfiguration.getOpMode(), 1)) {
                        chargingCurrentData.setChargePointStatus(ChargePointStatus.Preparing);
                        StatusNotificationReq statusNotificationReq = new StatusNotificationReq(chargingCurrentData.getConnectorId());
                        statusNotificationReq.sendStatusNotification();
                    }

                    if (Objects.equals(idTagInfo[0], chargingCurrentData.getIdTag())) {
                        // 로컬 목록에 있음
                        chargingCurrentData.setAuthorizeResult(true);
                        chargingCurrentData.setParentIdTag(idTagInfo[1]);
                        classUiProcess.setUiSeq(UiSeq.PLUG_CHECK);
                        activity.getFragmentChange().onFragmentChange(mChannel, UiSeq.PLUG_CHECK, "PLUG_CHECK", null);
                    } else if (Objects.equals(idTagInfo[0], "notFound")) {
                        // 목록에 없음(notFound)
                        AuthorizeReq authorizeReq = new AuthorizeReq(chargingCurrentData.getConnectorId());
                        authorizeReq.sendAuthorize(chargingCurrentData.getIdTag());
                    } else {
                        // 인증 실패
                        activity.getChargingCurrentData(mChannel).setAuthorizeResult(false);
                        classUiProcess.setUiSeq(UiSeq.MEMBER_CHECK_FAILED);
                        fragmentChange.onFragmentChange(mChannel, UiSeq.MEMBER_CHECK_FAILED, "MEMBER_CHECK_FAILED", null);
                        RxData rxData = activity.getControlBoard().getRxData(mChannel);
                        if (!rxData.isCsPilot() && Objects.equals(chargerConfiguration.getOpMode(), 1)) {
                            chargingCurrentData.setChargePointStatus(ChargePointStatus.Available);
                            StatusNotificationReq statusNotificationReq = new StatusNotificationReq(chargingCurrentData.getConnectorId());
                            statusNotificationReq.sendStatusNotification();
                        }
                    }
                }
            } else {
                // central system send
                SocketState state = socketReceiveMessage.getSocket().getState();
                if (state == SocketState.OPEN) {    // 서버 연결됨
                    if (Objects.equals(UiSeq.CHARGING, uiSeq)) {
                        if (Objects.equals(chargingCurrentData.getIdTag(), chargingCurrentData.getIdTagStop())) {
                            chargingCurrentData.setUserStop(true);
                        } else {
                            // 식별자가 상이할 경우 Authorize 전송
                            AuthorizeReq authorizeReq = new AuthorizeReq(chargingCurrentData.getConnectorId());
                            authorizeReq.sendAuthorize(chargingCurrentData.getIdTagStop());
                            fragmentChange.onFragmentChange(mChannel, UiSeq.CHARGING, "CHARGING", null);
                        }
                    } else {
                        if (chargingCurrentData.getChargePointStatus() == ChargePointStatus.Reserved) {
                            if (!Objects.equals(chargingCurrentData.getResIdTag(), chargingCurrentData.getIdTag())) {
                                Toast.makeText(getActivity(), "예약한 IdTag가 틀립니다. ", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        AuthorizeReq authorizeReq = new AuthorizeReq(chargingCurrentData.getConnectorId());
                        authorizeReq.sendAuthorize(chargingCurrentData.getIdTag());
                    }
                } else {
                    // 서버와 연결이 안된 경우
                    // isLocalAuthorizeOffline: 서버 연결이 끊겼을 때 오프라인 로컬 인증 허용 여부
                    if (GlobalVariables.isLocalAuthorizeOffline()) {
                        // local authorization enabled --> local 인증
                        idTagInfo = socketReceiveMessage.getLocalAuthorizationListStrings(uiSeq == UiSeq.CHARGING ?
                                chargingCurrentData.getIdTagStop() : chargingCurrentData.getIdTag());
                        if (Objects.equals(UiSeq.CHARGING, uiSeq)) {
                            if (Objects.equals(chargingCurrentData.getParentIdTag(), idTagInfo[1]) ||
                                    Objects.equals(chargingCurrentData.getIdTag(), chargingCurrentData.getIdTagStop())) {
                                chargingCurrentData.setUserStop(true);
                            } else {
                                activity.getFragmentChange().onFragmentChange(mChannel, UiSeq.CHARGING, "CHARGING", null);
                            }
                        } else {
                            // isAllowOfflineTxForUnknownId: 오프라인에서 미등록 IdTag도 거래 허용
                            if (Objects.equals(idTagInfo[0], chargingCurrentData.getIdTag()) || GlobalVariables.isAllowOfflineTxForUnknownId() ||
                                    GlobalVariables.isStopTransactionOnInvalidId()) {

                                chargingCurrentData.setParentIdTag(Objects.equals(idTagInfo[1], "") ? "미지원" : idTagInfo[1]);

                                AuthorizeReq authorizeReq = new AuthorizeReq(chargingCurrentData.getConnectorId());
                                authorizeReq.sendAuthorize(chargingCurrentData.getIdTag());
                                
                                chargingCurrentData.setChargePointStatus(ChargePointStatus.Preparing);
                                StatusNotificationReq statusNotificationReq = new StatusNotificationReq(chargingCurrentData.getConnectorId());
                                statusNotificationReq.sendStatusNotification();

                                // isStopTransactionOnInvalidId: 미등록 IdTag로 시작했으면 나중에 중단 사유 세팅
                                chargingCurrentData.setStopReason(!Objects.equals(idTagInfo[0], chargingCurrentData.getIdTag()) &&
                                        GlobalVariables.isStopTransactionOnInvalidId() ? Reason.DeAuthorized : chargingCurrentData.getStopReason());
                                activity.getClassUiProcess(mChannel).setUiSeq(UiSeq.PLUG_CHECK);
                                activity.getFragmentChange().onFragmentChange(mChannel, UiSeq.PLUG_CHECK, "PLUG_CHECK", null);
                            } else {
                                // 인증 실패
                                classUiProcess.setUiSeq(UiSeq.MEMBER_CHECK_FAILED);
                                fragmentChange.onFragmentChange(mChannel, UiSeq.MEMBER_CHECK_FAILED, "MEMBER_CHECK_FAILED", null);
                            }
                        }
                    } else {
                        Toast.makeText(getActivity(), "서버와 통신 DISCONNECT!!! 인증 실패. ", Toast.LENGTH_SHORT).show();
                        if (Objects.equals(UiSeq.CHARGING, uiSeq)) {
                            activity.getFragmentChange().onFragmentChange(mChannel,UiSeq.CHARGING, "CHARGING", null);
                        } else {
                            classUiProcess.setUiSeq(UiSeq.MEMBER_CHECK_FAILED);
                            fragmentChange.onFragmentChange(mChannel, UiSeq.MEMBER_CHECK_FAILED, "MEMBER_CHECK_FAILED", null);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("onViewCreated error : {}", e.getMessage());
        }
    }

    private void mediaPlayer() {
        releasePlayer();
        
        try {
            mediaPlayer = MediaPlayer.create(requireContext(), R.raw.membercardwait);
            mediaPlayer.setOnCompletionListener(me -> releasePlayer());
            mediaPlayer.start();
        } catch (Exception e) {
            logger.error("mediaPlayer error : {}", e.getMessage());
        }
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception e) {
                logger.error("releasePlayer error : {}", e.getMessage());
            }
            mediaPlayer = null;
        }
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