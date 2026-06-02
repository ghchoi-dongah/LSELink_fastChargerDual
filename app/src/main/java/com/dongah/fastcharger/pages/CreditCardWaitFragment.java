package com.dongah.fastcharger.pages;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.UiSeq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CreditCardWaitFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CreditCardWaitFragment extends Fragment implements View.OnClickListener {
    private static final Logger logger = LoggerFactory.getLogger(CreditCardWaitFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    int TIME_MAX = 20;
    int cnt = 0;
    boolean isFlag = false;
    TextView textViewCreditWaitMessage, textViewFailed, textViewConnectorRetryMessage;
    ImageView imageViewLoading, imageViewCreditFailed;
    AnimationDrawable animationDrawable;
    ObjectAnimator fadeAnimator;

    MainActivity activity;

    Handler countHandler;
    Runnable countRunnable;

    public CreditCardWaitFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment CreditCardWaitFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CreditCardWaitFragment newInstance(String param1, String param2) {
        CreditCardWaitFragment fragment = new CreditCardWaitFragment();
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
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_credit_card_wait, container, false);
        view.setOnClickListener(this);
        imageViewLoading = view.findViewById(R.id.imageViewLoading);
        imageViewLoading.setBackgroundResource(R.drawable.ani_loading);
        animationDrawable = (AnimationDrawable) imageViewLoading.getBackground();
        textViewCreditWaitMessage = view.findViewById(R.id.textViewCreditWaitMessage);
        imageViewCreditFailed = view.findViewById(R.id.imageViewCreditFailed);
        textViewFailed = view.findViewById(R.id.textViewFailed);
        textViewConnectorRetryMessage = view.findViewById(R.id.textViewConnectorRetryMessage);

        // textViewFailed animation
        fadeAnimator = ObjectAnimator.ofFloat(textViewFailed, "alpha", 1f, 0.2f);
        fadeAnimator.setDuration(1000);
        fadeAnimator.setRepeatCount(ValueAnimator.INFINITE);
        fadeAnimator.setRepeatMode(ValueAnimator.REVERSE);
        fadeAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        activity = ((MainActivity) MainActivity.mContext);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            isFlag = false;
            animationDrawable.start();

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    countHandler = new Handler();
                    countRunnable = new Runnable() {
                        @Override
                        public void run() {
                            try {
                                cnt++;
                                if (cnt > TIME_MAX) {
                                    countHandler.removeCallbacks(countRunnable);

                                    // 결제 실패
                                    creditFailed();
                                } else {
                                    countHandler.postDelayed(countRunnable, 1000);
                                }
                            } catch (Exception e) {
                                logger.error("onViewCreated run error : {}", e.getMessage());
                            }
                        }
                    };
                    countHandler.postDelayed(countRunnable, 1000);
                }
            });
        } catch (Exception e) {
            logger.error("onViewCreated error : {}", e.getMessage(), e);
        }
    }

    private void creditFailed() {
        try {
            textViewCreditWaitMessage.setText(R.string.creditCheckFailedMessage);
            animationDrawable.stop();
            imageViewLoading.setVisibility(View.INVISIBLE);
            imageViewCreditFailed.setVisibility(View.VISIBLE);
            textViewFailed.setVisibility(View.VISIBLE);
            textViewConnectorRetryMessage.setVisibility(View.VISIBLE);
            fadeAnimator.start();
            isFlag = true;
        } catch (Exception e) {
            logger.error("creditFailed error : {}", e.getMessage(), e);
        }
    }

    @Override
    public void onClick(View v) {
        try {
            if (!isAdded() && !isFlag) return;
            activity.getClassUiProcess(mChannel).onHome();
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
                countHandler.removeCallbacksAndMessages(null);
                countHandler = null;
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
        try {
            if (countHandler != null) {
                countHandler.removeCallbacks(countRunnable);
                countHandler.removeCallbacksAndMessages(null);
                countHandler.removeMessages(0);
            }
        } catch (Exception e) {
            logger.error("onDetach error : {}", e.getMessage());
        }
    }
}