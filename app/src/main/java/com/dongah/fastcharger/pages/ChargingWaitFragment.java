package com.dongah.fastcharger.pages;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.utils.SharedModel;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChargingWaitFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChargingWaitFragment extends Fragment implements View.OnClickListener {
    private static final Logger logger = LoggerFactory.getLogger(ChargingWaitFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    Handler countHandler;
    Runnable countRunnable;
    SharedModel sharedModel;
    String[] requestStrings = new String[1];
    ChargerConfiguration chargerConfiguration;
    ChargingCurrentData chargingCurrentData;
    CircularProgressIndicator progressCircular;

    public ChargingWaitFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChargingWaitFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChargingWaitFragment newInstance(String param1, String param2) {
        ChargingWaitFragment fragment = new ChargingWaitFragment();
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
        View view = inflater.inflate(R.layout.fragment_charging_wait, container, false);
        chargerConfiguration = ((MainActivity) MainActivity.mContext).getChargerConfiguration();
        chargingCurrentData = ((MainActivity) MainActivity.mContext).getChargingCurrentData(mChannel);
        progressCircular = view.findViewById(R.id.progressCircular);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            sharedModel = new ViewModelProvider(requireActivity()).get(SharedModel.class);
            requestStrings[0] = String.valueOf(mChannel);
            sharedModel.setMutableLiveData(requestStrings);
            showLoading();
        } catch (Exception e) {
            logger.error("ChargingWaitFragment onViewCreated error : {}", e.getMessage());
        }
    }

    @Override
    public void onClick(View v) {
        if (!isAdded()) return;
        ((MainActivity) MainActivity.mContext).getClassUiProcess(mChannel).setUiSeq(UiSeq.CHARGING);
        ((MainActivity) MainActivity.mContext).getFragmentChange().onFragmentChange(mChannel, UiSeq.CHARGING, "CHARGING", null);
    }

    private void showLoading() {
        progressCircular.setVisibility(View.VISIBLE);
        progressCircular.setIndeterminate(true);
    }

    private void hideLoading() {
        progressCircular.setVisibility(View.GONE);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        try {
            if (countHandler != null) {
                countHandler.removeCallbacks(countRunnable);
                countHandler.removeCallbacksAndMessages(null);
                countHandler.removeMessages(0);
            }
            hideLoading();
        } catch (Exception e) {
            logger.error("ChargingWaitFragment onDetach error : {}", e.getMessage());
        }
    }
}