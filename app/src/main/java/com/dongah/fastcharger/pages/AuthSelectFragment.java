package com.dongah.fastcharger.pages;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

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
 * Use the {@link AuthSelectFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AuthSelectFragment extends Fragment implements View.OnClickListener {
    private static final Logger logger = LoggerFactory.getLogger(AuthSelectFragment.class);


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    CardView cardViewMember, cardViewNoMember, cardViewCorp, cardViewMoe;
    TextView textViewMemberUnitInput, textViewNoMemberUnitInput, textViewCorpUnitInput, textViewMoeUnitInput;

    MainActivity activity;
    ClassUiProcess classUiProcess;
    FragmentChange fragmentChange;
    ChargingCurrentData chargingCurrentData;
    ChargerConfiguration chargerConfiguration;

    public AuthSelectFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AuthFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AuthSelectFragment newInstance(String param1, String param2) {
        AuthSelectFragment fragment = new AuthSelectFragment();
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
        View view = inflater.inflate(R.layout.fragment_auth_select, container, false);
        activity = (MainActivity) MainActivity.mContext;
        classUiProcess = activity.getClassUiProcess(mChannel);
        fragmentChange = activity.getFragmentChange();
        chargingCurrentData = activity.getChargingCurrentData(mChannel);
        chargerConfiguration = activity.getChargerConfiguration();

        cardViewMember = view.findViewById(R.id.cardViewMember);
        cardViewMember.setOnClickListener(this);
        cardViewNoMember = view.findViewById(R.id.cardViewNoMember);
        cardViewNoMember.setOnClickListener(this);
        cardViewCorp = view.findViewById(R.id.cardViewCorp);
        cardViewCorp.setOnClickListener(this);
        cardViewMoe = view.findViewById(R.id.cardViewMoe);
        cardViewMoe.setOnClickListener(this);
        textViewMemberUnitInput = view.findViewById(R.id.textViewMemberUnitInput);
        textViewNoMemberUnitInput = view.findViewById(R.id.textViewNoMemberUnitInput);
        textViewCorpUnitInput = view.findViewById(R.id.textViewCorpUnitInput);
        textViewMoeUnitInput = view.findViewById(R.id.textViewMoeUnitInput);

        return view;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            if (Objects.equals(chargerConfiguration.getAuthMode(), 1)) {
                // member + nomember
                cardViewCorp.setVisibility(View.INVISIBLE);
                cardViewMoe.setVisibility(View.INVISIBLE);
            } else if (Objects.equals(chargerConfiguration.getAuthMode(), 2)) {
                // member + nomember + corp + moe
                cardViewCorp.setVisibility(View.VISIBLE);
                cardViewMoe.setVisibility(View.VISIBLE);
            }
            textViewMemberUnitInput.setText(getString(R.string.price, GlobalVariables.userTypeM));
            textViewNoMemberUnitInput.setText(getString(R.string.price, GlobalVariables.userTypeN));
            textViewCorpUnitInput.setText(getString(R.string.price, GlobalVariables.userTypeC));
            textViewMoeUnitInput.setText(getString(R.string.price, GlobalVariables.userTypeK));
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
            } else if (Objects.equals(getId, R.id.cardViewNoMember)) {
                chargingCurrentData.setAuthType("N");
                chargingCurrentData.setPaymentType(PaymentType.CREDIT);
                chargingCurrentData.setPowerUnitPrice(GlobalVariables.userTypeN);
                classUiProcess.setUiSeq(UiSeq.CREDIT_CARD);
                fragmentChange.onFragmentChange(mChannel, UiSeq.CREDIT_CARD, "CREDIT_CARD", null);
            } else if (Objects.equals(getId, R.id.cardViewCorp)) {
                chargingCurrentData.setAuthType("C");
                chargingCurrentData.setPaymentType(PaymentType.CORP);
                chargingCurrentData.setPowerUnitPrice(GlobalVariables.userTypeC);
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

    @Override
    public void onDetach() {
        super.onDetach();
    }
}