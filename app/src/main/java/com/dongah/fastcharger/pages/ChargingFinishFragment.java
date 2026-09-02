package com.dongah.fastcharger.pages;

import android.annotation.SuppressLint;
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
import android.widget.Button;
import android.widget.TextView;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.smartro.VCatPaymentManager;
import com.dongah.fastcharger.utils.ToastPositionMake;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONObject;
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

    private static final long UI_CHECK_INTERVAL_MS = 5 * 60 * 1000; // 5분
    Button btnCheck;
    TextView textViewSocValue, textViewChargingAmtValue, textViewChargingTimeValue, textViewLimitSocValue;
    TextView textViewPrePayment, textViewInputPrePayment, textViewPartCancelPay, textViewInputCancelPayment, txtChargePay;
    CircularProgressIndicator progressCircular;

    MediaPlayer mediaPlayer;
    Handler uiCheckHandler;
    MainActivity activity;
    ChargerConfiguration chargerConfiguration;
    ChargingCurrentData chargingCurrentData;
    DecimalFormat powerFormatter = new DecimalFormat("#,###,##0.00");
    DecimalFormat payFormatter = new DecimalFormat("#,###,##0");
    int realPay;
    private boolean cancelRequested = false;


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
        activity = (MainActivity) MainActivity.mContext;
        chargerConfiguration = activity.getChargerConfiguration();
        chargingCurrentData = activity.getChargingCurrentData(mChannel);
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
        txtChargePay = view.findViewById(R.id.txtChargePay);
        return view;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            progressCircular.isIndeterminate();
            mediaPlayer();
            startUiCheckLoop();
            updateChargingUiAndPayment();

            // charging finish info
//            ((MainActivity) MainActivity.mContext).runOnUiThread(new Runnable() {
//                @SuppressLint("SetTextI18n")
//                @Override
//                public void run() {
//                    try {
//                        textViewSocValue.setText(chargingCurrentData.getSoc() + "%");
//                        progressCircular.setProgress(chargingCurrentData.getSoc(), true);
//                        textViewLimitSocValue.setText("목표 충전율: " +chargingCurrentData.getLimitSoc() + "%");
//                        textViewChargingAmtValue.setText(powerFormatter.format(chargingCurrentData.getPowerMeterUse() * 0.01) + "kWh");
//                        textViewChargingTimeValue.setText(chargingCurrentData.getChargingUseTime());
//                        realPay = (int) chargingCurrentData.getPowerMeterUsePay();
//                        txtChargePay.setText(payFormatter.format(realPay) + " 원");
//                    } catch (Exception e) {
//                        logger.error("onViewCreated charging result error : {}", e.getMessage(), e);
//                    }
//
//
//                    // TODO : 신용카드 결제 정산
//                    prepaymentInfo(chargingCurrentData.isPrePaymentResult());
////                    if (chargingCurrentData.isPrePaymentResult()) {
////
////                    }
//                }
//            });
        } catch (Exception e) {
            logger.error("onViewCreated error : {}", e.getMessage(), e);
        }
    }

    private void startUiCheckLoop() {
        // unplug check 후 초기 화면
        uiCheckHandler = new Handler();
        uiCheckHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!((MainActivity) MainActivity.mContext).getControlBoard().getRxData(mChannel).isCsPilot()) {
                    ((MainActivity) MainActivity.mContext).getClassUiProcess(mChannel).onHome();
                    return;
                }
                uiCheckHandler.postDelayed(this, UI_CHECK_INTERVAL_MS);
            }
        }, UI_CHECK_INTERVAL_MS);
    }

    private void stopUiCheckLoop() {
        if (uiCheckHandler != null) {
            uiCheckHandler.removeCallbacksAndMessages(null);
            uiCheckHandler = null;
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateChargingUiAndPayment() {
        try {
            if (chargingCurrentData == null) {
                logger.error("chargingCurrentData is null");
                return;
            }

            prepaymentInfo(chargingCurrentData.isPrePaymentResult());
            textViewSocValue.setText(chargingCurrentData.getSoc() + "%");
            progressCircular.setProgress(chargingCurrentData.getSoc(), true);
            textViewLimitSocValue.setText("목표 충전율: " +chargingCurrentData.getLimitSoc() + "%");
            textViewChargingAmtValue.setText(powerFormatter.format(chargingCurrentData.getPowerMeterUse() * 0.01) + "kWh");
            textViewChargingTimeValue.setText(chargingCurrentData.getChargingUseTime());
            realPay = (int) chargingCurrentData.getPowerMeterUsePay();
            txtChargePay.setText(payFormatter.format(realPay) + " 원");

            int prePayment  = chargingCurrentData.getPrePayment();
            int usedPayment = (int) chargingCurrentData.getPowerMeterUsePay();
            int gapAmount   = prePayment - usedPayment;

            if (chargingCurrentData.isPrePaymentResult() && gapAmount > 0 && !cancelRequested) {
                cancelRequested = true;
                chargingCurrentData.setPartCancelAmount(gapAmount);
                requestCancel(usedPayment);
            }
        } catch (Exception e) {
            logger.error("updateChargingUiAndPayment error : {}", e.getMessage(), e);
        }
    }

    // ── 취소 요청 (VCatPaymentManager 위임) ──────────────

    /**
     * VCatPaymentManager.requestChargeFinishCancel() 호출
     *
     * usedPayment == 0 → 내부에서 deal="cancellation" (전체취소)
     * usedPayment  > 0 → 내부에서 deal="partial-cancel" (부분취소)
     *
     * 완료 후 f2.req(NonMemberPartCancelPaymentReq) 자동 전송
     */
    private void requestCancel(int usedPayment) {
        if (!VCatPaymentManager.getInstance().isConnected()) {
            logger.error("requestCancel: V-CAT 미연결");
            showToast("결제 서비스 연결 중입니다. 잠시 후 다시 시도해주세요.");
            cancelRequested = false;
            return;
        }

        //
        ChargerConfiguration cfg = activity.getChargerConfiguration();
        int connectorId          = chargingCurrentData.getConnectorId();
        VCatPaymentManager.getInstance()
                .requestChargeFinishCancel(
                        usedPayment,
                        chargingCurrentData,
                        cfg,
                        connectorId,
                        new VCatPaymentManager.PaymentCallback() {
                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void onSuccess(JSONObject result) {
                                if (!isAdded() || getActivity() == null) return;

                                chargingCurrentData.setTradeCode(
                                        result.optString("response-code", ""));
                                chargingCurrentData.setTradeMethod(
                                        result.optString("display-msg", ""));

                                if (usedPayment == 0) {
                                    // ── 전체취소 성공 ──────────────────────────
                                    logger.info("전체취소 성공: approvalNo={}, amount={}",
                                            chargingCurrentData.getPartCancelNumber(),
                                            chargingCurrentData.getPartCancelAmount());

                                    ToastPositionMake toast = new ToastPositionMake(activity);
                                    toast.onShowToast(mChannel,"결제 전체취소 완료: "
                                            + payFormatter.format(chargingCurrentData.getPartCancelAmount())
                                            + " 원");
                                } else {
                                    // ── 부분취소 성공 ──────────────────────────
                                    logger.info("부분취소 성공: approvalNo={}, amount={}",
                                            chargingCurrentData.getPartCancelNumber(),
                                            chargingCurrentData.getPartCancelAmount());
                                    ToastPositionMake toast = new ToastPositionMake(activity);
                                    toast.onShowToast(mChannel,"결제 취소 금액: "
                                            + payFormatter.format(chargingCurrentData.getPartCancelAmount())
                                            + " 원");
                                }

                                // f2 전문 전송: 취소 결과 CSMS 전달 (성공)
//                                sendPartCancelResultToServer(connectorId, true);
                            }

                            @RequiresApi(api = Build.VERSION_CODES.O)
                            @Override
                            public void onFailure(String errorMessage) {
                                if (!isAdded() || getActivity() == null) return;

                                cancelRequested = false; // 재시도 허용

                                logger.error("취소 실패 (usedPayment={}): {}",
                                        usedPayment, errorMessage);

                                ToastPositionMake toast = new ToastPositionMake(activity);
                                toast.onShowToast(mChannel, usedPayment == 0
                                        ? "전체취소 실패: 고객운영 센터로 연락하세요!"
                                        : "부분취소 실패: 고객운영 센터로 연락하세요!");

                                // f2 전문 전송: 취소 결과 CSMS 전달 (실패)
                                // 규약: 실패 시 partCancel* 필드 초기화 후 transaction_id 만 전송
//                                sendPartCancelResultToServer(connectorId, false);
                            }
                        });
    }

    private void showToast(String message) {
        if (!isAdded() || getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            try {
                android.widget.Toast.makeText(requireContext(), message,
                        android.widget.Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                logger.error("showToast error: {}", e.getMessage());
            }
        });
    }

    private void prepaymentInfo(boolean check) {
        int visibility = check ? View.VISIBLE : View.GONE;

        textViewPrePayment.setVisibility(visibility);
        textViewInputPrePayment.setVisibility(visibility);
        textViewPartCancelPay.setVisibility(visibility);
        textViewInputCancelPayment.setVisibility(visibility);
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
            stopUiCheckLoop();
        } catch (Exception e) {
            logger.error("onDestroyView error : {}", e.getMessage(), e);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}