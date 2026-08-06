package com.dongah.fastcharger.pages;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.PaymentType;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.vcat.ServiceProcessingActivity;
import com.dongah.fastcharger.vcat.VCat;
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
    Animation animation;

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
        amountFormatter = new DecimalFormat("###,##0");
        imageViewCreditCard = view.findViewById(R.id.imageViewCreditCard);
        animation = AnimationUtils.loadAnimation(getContext(), R.anim.translate);

        return view;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            textViewTagTimer.setText(timer + "초");
            imageViewCreditCard.startAnimation(animation);

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

                        if (!chargingCurrentData.isPrePaymentResult()) {
                            activity.getServiceProcessingActivity().cancelService();
                        }

                        activity.getClassUiProcess(mChannel).onHome();
                    } else {
                        countHandler.postDelayed(countRunnable, 1000);
                        textViewTagTimer.setText(timer + "초");
                    }
                }
            };
            countHandler.postDelayed(countRunnable, 1000);

            // V-CAT 결제 요청 (NFC 카드 태깅 대기)
            if (!activity.getServiceProcessingActivity().isBound()) {
                logger.warn("ch={} V-CAT 미연결 - 결제 불가", mChannel);
            } else {
                try {
                    int amount = GlobalVariables.FullRechgAmt;
                    int tax    = Math.round(amount / 11.0f);

                    chargingCurrentData.setPaymentType(PaymentType.CREDIT);
                    chargingCurrentData.setPrePayment(amount);
                    chargingCurrentData.setSurtax(tax);
                    chargingCurrentData.setTip(0);

                    // V-CAT API V3.13 요청 빌드
                    VCat vcat        = new VCat(activity);
                    vcat.tranType    = VCat.TYPE_CREDIT;
                    vcat.tranDeal    = VCat.DEAL_APPROVAL;
                    vcat.totalAmount = String.valueOf(amount);
                    vcat.surtax      = String.valueOf(tax);
                    vcat.tip         = "0";
                    vcat.needCardNo  = true;
                    vcat.mid         = activity.getChargerConfiguration().getMID();
                    // vanComm: 운영 모드에 따라 실서버/테스트 서버 선택
                    vcat.vanComm     = (activity.getChargerConfiguration().getOpMode() == 0)
                                       ? VCat.SERVER_TEST : VCat.SERVER_REAL;

                    boolean sent = activity.getServiceProcessingActivity()
                            .executeService(vcat.buildRequest().toString());
                    if (sent) {
                        activity.getClassUiProcess(mChannel).setUiSeq(UiSeq.CREDIT_CARD_WAIT);
                        activity.getFragmentChange().onFragmentChange(
                                mChannel, UiSeq.CREDIT_CARD_WAIT, "CREDIT_CARD_WAIT", null);
                    } else {
                        logger.warn("ch={} V-CAT executeService 실패", mChannel);
                    }
                } catch (Exception e) {
                    logger.error("ch={} V-CAT 결제 요청 오류", mChannel, e);
                }
            }
        } catch (Exception e) {
            logger.error("onViewCreated error : {}", e.getMessage(), e);
        }
    }

    @Override
    public void onDestroyView() {
        try {
            if (countHandler != null) {
                countHandler.removeCallbacks(countRunnable);
                countHandler.removeCallbacksAndMessages(null);
            }

            if (animation != null) {
                imageViewCreditCard.clearAnimation();
                animation.setAnimationListener(null);
                animation = null;
            }
        } catch (Exception e) {
            logger.error("onDestroyView error : {}", e.getMessage(), e);
        }
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}