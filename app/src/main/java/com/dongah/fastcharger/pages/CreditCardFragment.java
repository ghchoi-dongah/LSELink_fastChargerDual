package com.dongah.fastcharger.pages;

import android.annotation.SuppressLint;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.GlobalVariables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CreditCardFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CreditCardFragment extends Fragment {
    private static final Logger logger = LoggerFactory.getLogger(CreditCardFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    int timer = 40;
    TextView txtInputAmt, textViewTagTimer;
    ImageView imageViewCreditCard;
    AnimationDrawable animationDrawable;

    DecimalFormat amountFormatter;
    Handler countHandler;
    Runnable countRunnable;

    MainActivity activity;
    ChargingCurrentData chargingCurrentData;


    public CreditCardFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CreditCardFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CreditCardFragment newInstance(String param1, String param2) {
        CreditCardFragment fragment = new CreditCardFragment();
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
        View view = inflater.inflate(R.layout.fragment_credit_card, container, false);
        activity= (MainActivity) MainActivity.mContext;
        chargingCurrentData = activity.getChargingCurrentData(mChannel);

        textViewTagTimer = view.findViewById(R.id.textViewTagTimer);
        txtInputAmt = view.findViewById(R.id.txtInputAmt);
        imageViewCreditCard = view.findViewById(R.id.imageViewCreditCard);
        imageViewCreditCard.setBackgroundResource(R.drawable.creditcardtagging);
        animationDrawable = (AnimationDrawable) imageViewCreditCard.getBackground();
        amountFormatter = new DecimalFormat("###,##0");

        // TODO creditcardtagging
        return view;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            animationDrawable.start();
            textViewTagTimer.setText(timer + "초");

            try {
                txtInputAmt.setText(amountFormatter.format(GlobalVariables.FullRechgAmt)); // 완충기준 충전금액
            } catch (Exception e) {
                txtInputAmt.setText(amountFormatter.format(1500));
                logger.error("onViewCreated fullRechgAmt error", e);
            }

            countHandler = new Handler();
            countRunnable = new Runnable() {
                @Override
                public void run() {
                    timer--;
                    if (timer <= 0) {
                        countHandler.removeCallbacks(countRunnable);
                        countHandler.removeCallbacksAndMessages(null);
                        countHandler.removeMessages(0);

                        if (chargingCurrentData.isPrePaymentResult()) {
                            //TODO: 선 결제에 의한 무카드 취소
                        }

                        activity.getClassUiProcess(mChannel).onHome();
                    } else {
                        countHandler.postDelayed(countRunnable, 1000);
                        textViewTagTimer.setText(timer + "초");
                    }
                }
            };
            countHandler.postDelayed(countRunnable, 1000);

            // TODO: 신용카드 결제
        } catch (Exception e) {
            logger.error("onViewCreated error : {}", e.getMessage(), e);
        }
    }

    @Override
    public void onDestroyView() {
        try {
            if (animationDrawable != null) {
                animationDrawable.stop();
            }

            if (imageViewCreditCard != null) {
                Drawable bg = imageViewCreditCard.getBackground();
                if (bg instanceof AnimationDrawable) {
                    ((AnimationDrawable) bg).stop();
                }
                imageViewCreditCard.setBackground(null);
            }

            if (countHandler != null) {
                countHandler.removeCallbacks(countRunnable);
                countHandler.removeCallbacksAndMessages(null);
                countHandler.removeMessages(0);
            }
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