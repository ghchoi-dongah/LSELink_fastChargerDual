package com.dongah.fastcharger.pages;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargerPointType;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.controlboard.RxData;
import com.dongah.fastcharger.controlboard.TxData;
import com.dongah.fastcharger.sqlite.SQLiteHelper;
import com.dongah.fastcharger.utils.SharedModel;
import com.dongah.fastcharger.websocket.socket.SocketState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link InitFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class InitFragment extends Fragment implements View.OnClickListener {
    private static final Logger logger = LoggerFactory.getLogger(InitFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    Animation animBlink;
    View viewCircle;
    TextView textViewInitMessage, textViewConnector, textViewInfo;
    ImageView imageViewBus, imageViewFault;

    MainActivity activity;
    ChargerConfiguration chargerConfiguration;
    ChargingCurrentData chargingCurrentData;
    TxData txData;
    SharedModel sharedModel;
    String[] requestStrings = new String[1];

    Handler handler;
    Runnable runnable;
    RxData rxData;

    Handler eventHandler;
    Runnable eventRunnable;

    public InitFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment InitFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static InitFragment newInstance(String param1, String param2) {
        InitFragment fragment = new InitFragment();
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

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_init, container, false);
        animBlink = AnimationUtils.loadAnimation(getActivity(), R.anim.blink_animation);
        activity = ((MainActivity) MainActivity.mContext);
        chargerConfiguration = activity.getChargerConfiguration();
        chargingCurrentData = activity.getChargingCurrentData(mChannel);
        txData = activity.getControlBoard().getTxData(mChannel);
        rxData = activity.getControlBoard().getRxData(mChannel);

        textViewInitMessage = view.findViewById(R.id.textViewInitMessage);
        textViewInitMessage.startAnimation(animBlink);
        textViewConnector = view.findViewById(R.id.textViewConnector);
        imageViewBus = view.findViewById(R.id.imageViewBus);
        viewCircle = view.findViewById(R.id.viewCircle);
        viewCircle.setOnClickListener(this);
        imageViewFault = view.findViewById(R.id.imageViewFault);
        textViewInfo = view.findViewById(R.id.textViewInfo);

        try {
            if (chargingCurrentData.isConnectUse()) {
                textViewInitMessage.setText(R.string.initMessage);
                imageViewFault.setVisibility(View.INVISIBLE);
            } else {
                textViewInitMessage.setText(R.string.changeModeMessage);
                imageViewFault.setVisibility(View.VISIBLE);
            }

            if (mChannel == 0) {
                imageViewBus.setScaleX(1f);
                textViewConnector.setText("1 커넥터");
            } else {
                imageViewBus.setScaleX(-1f);
                textViewConnector.setText("2 커넥터");
            }
        } catch (Exception e) {
            logger.error("onCreateView error : {}", e.getMessage(), e);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            sharedModel = new ViewModelProvider(requireActivity()).get(SharedModel.class);
            requestStrings[0] = String.valueOf(0);
            sharedModel.setMutableLiveData(requestStrings);

            if (chargerConfiguration.isInitInfo()) {
                textViewInfo.setVisibility(View.VISIBLE);
                textViewInfo.setText(getString(R.string.intiInfo, chargingCurrentData.getLimitSoc(),
                        txData.getOutPowerLimit()));
            } else {
                textViewInfo.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            logger.error("onViewCreated error : {}", e.getMessage(), e);
        }
    }

    @Override
    public void onClick(View v) {
        if (!chargingCurrentData.isConnectUse()
                || (!Objects.equals(v.getId(), R.id.viewCircle) && !rxData.isCsPilot())) {
            return;
        }
        changeFragment();
    }

    private void initData() {
        try {
            chargingCurrentData.onCurrentDataClear();   // clear
            chargingCurrentData.setConnectorId(mChannel + 1);
            chargingCurrentData.setChargerPointType(ChargerPointType.COMBO);
        } catch (Exception e) {
            logger.error("initData error : {}", e.getMessage());
        }
    }

    private void changeFragment() {
        try {
            initData();

            if (Objects.equals(chargerConfiguration.getOpMode(), 0)) {
                // test mode
                double testPrice = Double.parseDouble(chargerConfiguration.getTestPrice());
                chargingCurrentData.setPowerUnitPrice(testPrice);
                activity.getClassUiProcess(mChannel).setUiSeq(UiSeq.PLUG_CHECK);
                activity.getFragmentChange().onFragmentChange(mChannel, UiSeq.PLUG_CHECK, "PLUG_CHECK", null);
            } else if (Objects.equals(chargerConfiguration.getOpMode(), 1)) {
                // server mode
                if (!onUnitPrice()) {
                    Toast.makeText(getActivity(), "단가 정보가 없습니다. \n잠시 후, 충전하세요!", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    SocketState socketState = activity.getSocketReceiveMessage().getSocket().getState();
                    if (Objects.equals(socketState, SocketState.OPEN)) {
                        if (chargerConfiguration.getAuthMode() == 0) {
                            activity.getClassUiProcess(mChannel).setUiSeq(UiSeq.MEMBER_CARD);
                            activity.getFragmentChange().onFragmentChange(mChannel, UiSeq.MEMBER_CARD, "MEMBER_CARD", null);
                        } else {
                            activity.getClassUiProcess(mChannel).setUiSeq(UiSeq.AUTH_SELECT);
                            activity.getFragmentChange().onFragmentChange(mChannel, UiSeq.AUTH_SELECT, "AUTH_SELECT", null);
                        }
                    } else {
                        activity.getToastPositionMake().onShowToast(mChannel, "서버 연결 DISCONNECT. \n충전을 할 수 없습니다.");
                    }
                } catch (Exception e){
                    activity.getToastPositionMake().onShowToast(mChannel, "서버 연결 DISCONNECT. \n충전을 할 수 없습니다.");
                    logger.error("server disconnect error : {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("changeFragment error : {}", e.getMessage());
        }
    }

    private boolean onUnitPrice() {
        try {
            SQLiteHelper helper = SQLiteHelper.getInstance(activity);
            if (!helper.isTableExists(helper, "CP_UNIT_PRICE")) {
                return false;
            }

            Cursor cursor = helper.selectAll("CP_UNIT_PRICE");
            return cursor != null && cursor.moveToFirst();
        } catch (Exception e){
            logger.error("onUnitPrice error : {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        try {
            requestStrings[0] = String.valueOf(mChannel);
            sharedModel.setMutableLiveData(requestStrings);

            if (handler != null) {
                handler.removeCallbacks(runnable);
                handler.removeCallbacksAndMessages(null);
                handler.removeMessages(0);
            }

            if (eventHandler != null) {
                eventHandler.removeCallbacks(eventRunnable);
            }
        } catch (Exception e) {
            logger.error("onDetach error : {}", e.getMessage());
        }
    }
}