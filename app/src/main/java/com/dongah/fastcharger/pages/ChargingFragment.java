package com.dongah.fastcharger.pages;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.controlboard.TxData;
import com.dongah.fastcharger.utils.SharedModel;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChargingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChargingFragment extends Fragment implements View.OnClickListener {
    private static final Logger logger = LoggerFactory.getLogger(ChargingFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    Button btnChargingStop;
    TextView textViewSocValue, textViewLimitSocValue, textViewLimitKwValue, textViewCarNum;
    TextView textViewChargingAmtValue, textViewChargingTimeRemainValue, textViewChargingTimeValue;
    TextView textViewChargingVoltageValue, textViewChargingPowerValue, textViewChargingCurrentValue, textViewRequestCurrentValue;
    CircularProgressIndicator progressCircular;

    MediaPlayer mediaPlayer;
    SharedModel sharedModel;
    String[] requestStrings = new String[1];
    Handler uiUpdateHandler;
    MainActivity activity;
    ChargerConfiguration chargerConfiguration;
    ChargingCurrentData chargingCurrentData;
    TxData txData;

    Date startTime = null, useTime = null;
    DecimalFormat powerFormatter = new DecimalFormat("#,###,##0.00");
    DecimalFormat voltageFormatter = new DecimalFormat("#,###,##0.0");
    ZonedDateTimeConvert zonedDateTimeConvert = new ZonedDateTimeConvert();

    public ChargingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChargingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChargingFragment newInstance(String param1, String param2) {
        ChargingFragment fragment = new ChargingFragment();
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
        View view = inflater.inflate(R.layout.fragment_charging, container, false);
        activity = ((MainActivity) MainActivity.mContext);
        chargerConfiguration = activity.getChargerConfiguration();
        chargingCurrentData = activity.getChargingCurrentData(mChannel);
        txData = activity.getControlBoard().getTxData(mChannel);
        btnChargingStop = view.findViewById(R.id.btnChargingStop);
        btnChargingStop.setOnClickListener(this);
        textViewSocValue = view.findViewById(R.id.textViewSocValue);
        textViewChargingAmtValue = view.findViewById(R.id.textViewChargingAmtValue);
        textViewChargingTimeRemainValue = view.findViewById(R.id.textViewChargingTimeRemainValue);
        textViewChargingTimeValue = view.findViewById(R.id.textViewChargingTimeValue);
        textViewChargingVoltageValue = view.findViewById(R.id.textViewChargingVoltageValue);
        textViewChargingPowerValue = view.findViewById(R.id.textViewChargingPowerValue);
        textViewChargingCurrentValue = view.findViewById(R.id.textViewChargingCurrentValue);
        textViewRequestCurrentValue = view.findViewById(R.id.textViewRequestCurrentValue);
        textViewLimitSocValue = view.findViewById(R.id.textViewLimitSocValue);
        textViewLimitKwValue = view.findViewById(R.id.textViewLimitKwValue);
        progressCircular = view.findViewById(R.id.progressCircular);
        return view;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            sharedModel = new ViewModelProvider(requireActivity()).get(SharedModel.class);
            requestStrings[0] = String.valueOf(mChannel);
            sharedModel.setMutableLiveData(requestStrings);
            progressCircular.setIndeterminate(false);
            mediaPlayer();      // media player

            try {
                textViewSocValue.setText(chargingCurrentData.getSoc() + "%");
                textViewLimitKwValue.setText(txData.getOutPowerLimit() + "kW");
                textViewLimitSocValue.setText("목표 충전율: " +chargingCurrentData.getLimitSoc() + "%");
                progressCircular.setProgress(chargingCurrentData.getSoc(), true);
                startTime = zonedDateTimeConvert.doStringDateToDate(chargingCurrentData.getChargingStartTime());

                if (Objects.equals(chargerConfiguration.getOpMode(), 1)) {
                    textViewCarNum.setText(getString(R.string.carNum) + chargingCurrentData.getParentIdTag());
                } else {
                    textViewCarNum.setText(getString(R.string.carNum) + "테스트 모드");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            onCharging();
        } catch (Exception e) {
            logger.error("onViewCreated error : {}", e.getMessage());
        }
    }

    @Override
    public void onClick(View v) {
        if (Objects.equals(v.getId(), R.id.btnChargingStop)) {
            ((MainActivity) MainActivity.mContext).getChargingCurrentData(mChannel).setUserStop(true);
        }
    }
    
    private void onCharging() {
        uiUpdateHandler = new Handler();
        uiUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((MainActivity) MainActivity.mContext).runOnUiThread(new Runnable() {
                     @SuppressLint({"SetTextI18n", "DefaultLocale"})
                     @RequiresApi(api = Build.VERSION_CODES.O)
                     @Override
                     public void run() {
                         try {
                             long diffTime = 0;
                             useTime = zonedDateTimeConvert.doStringDateToDate(zonedDateTimeConvert.getStringCurrentTimeZone());

                             if (useTime != null) {
                                 diffTime = (useTime.getTime() - startTime.getTime()) / 1000;
                                 int hour = (int) diffTime / 3600;
                                 int minute = (int) (diffTime % 3600) / 60;
                                 int second = (int) diffTime % 60;
                                 chargingCurrentData.setChargingTime((int) diffTime);
                                 textViewChargingTimeValue.setText(String.format("%02d", hour) + ":" + String.format("%02d", minute) + ":" + String.format("%02d", second));
                                 chargingCurrentData.setChargingUseTime(textViewChargingTimeValue.getText().toString());

                                 textViewChargingAmtValue.setText(powerFormatter.format(chargingCurrentData.getPowerMeterUse() * 0.01) + "kWh");

                                 int rHour = chargingCurrentData.getRemaintime() / 3600;
                                 int rMinute = (chargingCurrentData.getRemaintime() % 3600) / 60;
                                 int rSecond = chargingCurrentData.getRemaintime() % 60;

                                 textViewChargingTimeRemainValue.setText(String.format("%02d", rHour) + ":" + String.format("%02d", rMinute) + ":" + String.format("%02d", rSecond));

                                 textViewSocValue.setText(chargingCurrentData.getSoc() + "%");
                                 progressCircular.setProgress(chargingCurrentData.getSoc(), true);

                                 textViewChargingVoltageValue.setText(voltageFormatter.format(chargingCurrentData.getOutPutVoltage() * 0.1) + " V");
                                 textViewChargingCurrentValue.setText(powerFormatter.format(chargingCurrentData.getOutPutCurrent() * 0.1) + " A");
                                 textViewChargingPowerValue.setText(powerFormatter.format(chargingCurrentData.getOutPutVoltage() * chargingCurrentData.getOutPutCurrent() * 0.00001) + " kW");
                                 textViewRequestCurrentValue.setText(powerFormatter.format(chargingCurrentData.getTargetCurrent() * 0.1) + "A");
                             }
                         } catch (Exception e) {
                             logger.error("onCharging error : {}", e.getMessage());
                         }
                     }
                 });
                uiUpdateHandler.postDelayed(this, 1000);
            }
        }, 50);
    }
    
    private void mediaPlayer() {
        releasePlayer();
        
        try {
            mediaPlayer = MediaPlayer.create(requireContext(), R.raw.charging);
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
        super.onDestroyView();
        try {
            if (uiUpdateHandler != null) {
                uiUpdateHandler.removeCallbacksAndMessages(null);
                uiUpdateHandler = null;
            }
        } catch (Exception e) {
            logger.error("onDestroyView error : {}", e.getMessage(), e);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        try {
            requestStrings[0] = String.valueOf(mChannel);
            sharedModel.setMutableLiveData(requestStrings);
        } catch (Exception e) {
            logger.error("onDetach error : {}", e.getMessage());
        }
    }
}