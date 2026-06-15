package com.dongah.fastcharger.pages;

import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChargingFinishWaitFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChargingFinishWaitFragment extends Fragment {
    private static final Logger logger = LoggerFactory.getLogger(ChargingFinishWaitFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    private static final int TIME_OUT = 10;
    int cnt;
    ImageView imageViewLoading;
    AnimationDrawable animationDrawable;
    Handler countHandler;
    Runnable countRunnable;
    ChargingCurrentData chargingCurrentData;

    public ChargingFinishWaitFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChargingFinishWaitFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChargingFinishWaitFragment newInstance(String param1, String param2) {
        ChargingFinishWaitFragment fragment = new ChargingFinishWaitFragment();
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
        View view = inflater.inflate(R.layout.fragment_charging_finish_wait, container, false);
        chargingCurrentData = ((MainActivity) MainActivity.mContext).getChargingCurrentData(mChannel);
        imageViewLoading = view.findViewById(R.id.imageViewLoading);
        imageViewLoading.setBackgroundResource(R.drawable.ani_loading);
        animationDrawable = (AnimationDrawable) imageViewLoading.getBackground();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            cnt = 0;
            animationDrawable.start();

//            countHandler = new Handler(Looper.getMainLooper());
//            countRunnable = new Runnable() {
//                @Override
//                public void run() {
//                    cnt++;
//                    if (Objects.equals(cnt, TIME_OUT)) {
//
//                    } else {
//                        countHandler.postDelayed(countRunnable, 1000);
//                    }
//                }
//            };
//            countHandler.postDelayed(countRunnable, 1000);
        } catch (Exception e) {
            logger.error("onViewCreated error : {}", e.getMessage());
        }
    }

    @Override
    public void onDestroyView() {
        try {
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