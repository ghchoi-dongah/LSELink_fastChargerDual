package com.dongah.fastcharger.pages;

import android.annotation.SuppressLint;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.ClassUiProcess;
import com.dongah.fastcharger.basefunction.FragmentChange;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.PaymentType;
import com.dongah.fastcharger.basefunction.UiSeq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AuthSelect2Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AuthSelect2Fragment extends Fragment implements View.OnClickListener {
    private static final Logger logger = LoggerFactory.getLogger(AuthSelect2Fragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    CardView cardViewMember, cardViewMoe;
    TextView textViewMemberUnitInput, textViewMoeUnitInput;

    MainActivity activity;
    ClassUiProcess classUiProcess;
    FragmentChange fragmentChange;
    ChargingCurrentData chargingCurrentData;
    ChargerConfiguration chargerConfiguration;
    Handler uiCheckHandler;

    public AuthSelect2Fragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AuthSelect2Fragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AuthSelect2Fragment newInstance(String param1, String param2) {
        AuthSelect2Fragment fragment = new AuthSelect2Fragment();
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
        View view = inflater.inflate(R.layout.fragment_auth_select2, container, false);
        activity = (MainActivity) MainActivity.mContext;
        classUiProcess = activity.getClassUiProcess(mChannel);
        fragmentChange = activity.getFragmentChange();
        chargingCurrentData = activity.getChargingCurrentData(mChannel);
        chargerConfiguration = activity.getChargerConfiguration();

        cardViewMember = view.findViewById(R.id.cardViewMember);
        cardViewMember.setOnClickListener(this);;
        cardViewMoe = view.findViewById(R.id.cardViewMoe);
        cardViewMoe.setOnClickListener(this);
        textViewMemberUnitInput = view.findViewById(R.id.textViewMemberUnitInput);
        textViewMoeUnitInput = view.findViewById(R.id.textViewMoeUnitInput);
        return view;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            // 각 영역에 다른 색 적용
            setCardBorderColor(view.findViewById(R.id.layoutMoe),    R.color.green);
            setCardBorderColor(view.findViewById(R.id.layoutMember), R.color.primary);

            textViewMemberUnitInput.setText(getString(R.string.price, GlobalVariables.userTypeM));
            textViewMoeUnitInput.setText(getString(R.string.price, GlobalVariables.userTypeK));

            uiCheckHandler = new Handler();
            uiCheckHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    activity.getClassUiProcess(mChannel).onHome();
                }
            }, 60000);
        } catch (Exception e) {
            logger.error("onViewCreated error : {}", e.getMessage(), e);
        }
    }

    @Override
    public void onClick(View v) {
        try {
            int getId = v.getId();
            if (Objects.equals(getId, R.id.cardViewMember)) {
                chargingCurrentData.setAuthType("M");
                chargingCurrentData.setPaymentType(PaymentType.MEMBER);
                chargingCurrentData.setPowerUnitPrice(GlobalVariables.userTypeM);
                classUiProcess.setUiSeq(UiSeq.MEMBER_CARD);
                fragmentChange.onFragmentChange(mChannel, UiSeq.MEMBER_CARD, "MEMBER_CARD", null);
            } else if (Objects.equals(getId, R.id.cardViewMoe)) {
                chargingCurrentData.setAuthType("K");
                chargingCurrentData.setPaymentType(PaymentType.MOE);
                chargingCurrentData.setPowerUnitPrice(GlobalVariables.userTypeK);
                classUiProcess.setUiSeq(UiSeq.MEMBER_CARD);
                fragmentChange.onFragmentChange(mChannel, UiSeq.MEMBER_CARD, "MEMBER_CARD", null);
            }
        } catch (Exception e) {
            logger.error("onClick error : {}", e.getMessage(), e);
        }
    }

    // stroke를 적용하는 헬퍼 메서드
    private void setCardBorderColor(View view, int colorRes) {
        LayerDrawable layerDrawable = (LayerDrawable) view.getBackground().mutate();
        GradientDrawable shape = (GradientDrawable) layerDrawable.getDrawable(1);
        shape.setStroke(2, ContextCompat.getColor(requireContext(), colorRes));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            if (uiCheckHandler != null) {
                uiCheckHandler.removeCallbacksAndMessages(null);
                uiCheckHandler = null;
            }
        } catch (Exception e) {
            logger.error("onDestroyView error : {}", e.getMessage(), e);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}