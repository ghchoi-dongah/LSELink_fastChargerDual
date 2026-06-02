package com.dongah.fastcharger.pages;

import android.annotation.SuppressLint;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChargingFinishFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChargingFinishFragment extends Fragment implements View.OnClickListener {
    private static final Logger logger = LoggerFactory.getLogger(ChargingFinishFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    Button btnCheck;
    TextView textViewSocValue, textViewChargingAmtValue, textViewChargingTimeValue, textViewLimitSocValue;
    TextView textViewPrePayment, textViewInputPrePayment, textViewPartCancelPay, textViewInputCancelPayment;
    CircularProgressIndicator progressCircular;

    MediaPlayer mediaPlayer;
    Handler uiCheckHandler;
    ChargerConfiguration chargerConfiguration;
    ChargingCurrentData chargingCurrentData;
    DecimalFormat powerFormatter = new DecimalFormat("#,###,##0.00");


    public ChargingFinishFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChargingFinishFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChargingFinishFragment newInstance(String param1, String param2) {
        ChargingFinishFragment fragment = new ChargingFinishFragment();
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
        View view = inflater.inflate(R.layout.fragment_charging_finish, container, false);
        chargerConfiguration = ((MainActivity) MainActivity.mContext).getChargerConfiguration();
        chargingCurrentData = ((MainActivity) MainActivity.mContext).getChargingCurrentData(mChannel);
        btnCheck = view.findViewById(R.id.btnCheck);
        btnCheck.setOnClickListener(this);
        textViewSocValue = view.findViewById(R.id.textViewSocValue);
        textViewChargingAmtValue = view.findViewById(R.id.textViewChargingAmtValue);
        textViewChargingTimeValue = view.findViewById(R.id.textViewChargingTimeValue);
        progressCircular = view.findViewById(R.id.progressCircular);
        textViewLimitSocValue = view.findViewById(R.id.textViewLimitSocValue);
        textViewPrePayment = view.findViewById(R.id.textViewPrePayment);
        textViewInputPrePayment = view.findViewById(R.id.textViewInputPrePayment);
        textViewPartCancelPay = view.findViewById(R.id.textViewPartCancelPay);
        textViewInputCancelPayment = view.findViewById(R.id.textViewInputCancelPayment);
        return view;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            progressCircular.isIndeterminate();
            mediaPlayer();

            // unplug check 후 초기 화면
            uiCheckHandler = new Handler();
            uiCheckHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!((MainActivity) MainActivity.mContext).getControlBoard().getRxData(mChannel).isCsPilot()) {
                        btnCheck.performClick();
                    }
                    uiCheckHandler.postDelayed(this, 60000);
                }
            }, 60000);

            // charging finish info
            ((MainActivity) MainActivity.mContext).runOnUiThread(new Runnable() {
                @SuppressLint("SetTextI18n")
                @Override
                public void run() {
                    textViewSocValue.setText(chargingCurrentData.getSoc() + "%");
                    progressCircular.setProgress(chargingCurrentData.getSoc(), true);
                    textViewLimitSocValue.setText("목표 충전율: " +chargingCurrentData.getLimitSoc() + "%");
                    textViewChargingAmtValue.setText(powerFormatter.format(chargingCurrentData.getPowerMeterUse() * 0.01) + "kWh");
                    textViewChargingTimeValue.setText(chargingCurrentData.getChargingUseTime());

                    // 신용카드 결제
                    prepaymentInfo(chargingCurrentData.isPrePaymentResult());
                    if (chargingCurrentData.isPrePaymentResult()) {

                    }
                }
            });
        } catch (Exception e) {
            logger.error("ChargingFinishFragment onViewCreated error : {}", e.getMessage(), e);
        }
    }

    private void prepaymentInfo(boolean check) {
        textViewPrePayment.setVisibility(check ? View.VISIBLE : View.GONE);
        textViewInputPrePayment.setVisibility(check ? View.VISIBLE : View.GONE);
        textViewPartCancelPay.setVisibility(check ? View.VISIBLE : View.GONE);
        textViewInputCancelPayment.setVisibility(check ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View v) {
        if (!isAdded()) return;

        if (Objects.equals(v.getId(), R.id.btnCheck)) {
            ((MainActivity) MainActivity.mContext).getClassUiProcess(mChannel).onHome();
        }
    }

    private void mediaPlayer() {
        releasePlayer();

        try {
            mediaPlayer = MediaPlayer.create(requireContext(), R.raw.chargingfinsih);
            mediaPlayer.setOnCompletionListener(me -> releasePlayer());
            mediaPlayer.start();
        } catch (Exception e) {
            logger.error("ChargingFinishFragment mediaPlayer error : {}", e.getMessage());
        }
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception e) {
                logger.error("ChargingFinishFragment releasePlayer error : {}", e.getMessage());
            }
            mediaPlayer = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        try {
            if (uiCheckHandler != null) {
                uiCheckHandler.removeCallbacksAndMessages(null);
                uiCheckHandler.removeMessages(0);
            }
        } catch (Exception e) {
            logger.error("ChargingFinishFragment onDetach error : {}", e.getMessage());
        }
    }
}