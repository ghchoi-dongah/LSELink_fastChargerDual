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
import com.dongah.fastcharger.utils.BitUtilities;
import com.dongah.fastcharger.utils.SharedModel;
import com.dongah.fastcharger.websocket.ocpp.core.ChargePointErrorCode;
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
 * Use the {@link ConnectorCheckFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConnectorCheckFragment extends Fragment implements View.OnClickListener {
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
    boolean isFlag = false, isFlagAuthorize = true;
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
        view.setOnClickListener(this);
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
            rxData = activity.getControlBoard().getRxData(mChannel);
            txData = activity.getControlBoard().getTxData(mChannel);
            cnt = 0;
            isFlag = false;
            isFlagAuthorize = true;
            animationDrawable.start();

            // connection time out
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    countHandler = new Handler();
                    countRunnable = new Runnable() {
                        @Override
                        public void run() {
                            cnt++;
                            if (cnt >= GlobalVariables.getConnectionTimeOut()) {
                                // 충전기 종료
                                txData.setStart(false);
                                txData.setStop(true);
                                countHandler.removeCallbacks(countRunnable);

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
                            } else {
                                countHandler.postDelayed(countRunnable, 1000);
                            }

                            // connecting wait
                            if (rxData.isCsPilot()) {
                                if (textViewConnectorCheckMessage.getTag() == null || !(boolean) textViewConnectorCheckMessage.getTag()) {
                                    textViewConnectorCheckMessage.setText(R.string.EVCheckMessage);
                                    textViewConnectorCheckMessage.setTag(true);
                                }
                            }
                        }
                    };
                    countHandler.postDelayed(countRunnable, 1000);
                }
            });
        } catch (Exception e) {
            logger.error("onViewCreated error : {}", e.getMessage());
        }
    }

    @Override
    public void onClick(View v) {
        try {
            return;
        } catch (Exception e) {
            logger.error("onClick error : {}", e.getMessage());
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