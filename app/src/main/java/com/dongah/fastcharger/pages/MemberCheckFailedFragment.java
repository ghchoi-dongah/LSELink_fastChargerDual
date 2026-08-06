package com.dongah.fastcharger.pages;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MemberCheckFailedFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MemberCheckFailedFragment extends Fragment implements View.OnClickListener {
    private static final Logger logger = LoggerFactory.getLogger(MemberCheckFailedFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    private static final long UI_CHECK_INTERVAL_MS = 2 * 60 * 1000; // 2분
    TextView textViewFailed;
    ObjectAnimator fadeAnimator;
    Handler uiCheckHandler;

    public MemberCheckFailedFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MemberCheckFailedFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MemberCheckFailedFragment newInstance(String param1, String param2) {
        MemberCheckFailedFragment fragment = new MemberCheckFailedFragment();
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
        View view = inflater.inflate(R.layout.fragment_member_check_failed, container, false);
        view.setOnClickListener(this);
        textViewFailed = view.findViewById(R.id.textViewFailed);

        // textViewFailed animation
        fadeAnimator = ObjectAnimator.ofFloat(textViewFailed, "alpha", 1f, 0.2f);
        fadeAnimator.setDuration(1000);
        fadeAnimator.setRepeatCount(ValueAnimator.INFINITE);
        fadeAnimator.setRepeatMode(ValueAnimator.REVERSE);
        fadeAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeAnimator.start();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            // unplug check 후 초기 화면
            uiCheckHandler = new Handler();
            uiCheckHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!((MainActivity) MainActivity.mContext).getControlBoard().getRxData(mChannel).isCsPilot()) {
                        ((MainActivity) MainActivity.mContext).getClassUiProcess(mChannel).onHome();
                    }
                    uiCheckHandler.postDelayed(this, UI_CHECK_INTERVAL_MS);
                }
            }, UI_CHECK_INTERVAL_MS);
        } catch (Exception e) {
            logger.error("onViewCreated error : {}", e.getMessage(), e);
        }
    }

    @Override
    public void onClick(View v) {
        if (!isAdded()) return;
        ((MainActivity) MainActivity.mContext).getClassUiProcess(mChannel).onHome();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        try {
            if (fadeAnimator != null) {
                fadeAnimator.cancel();
                fadeAnimator = null;
            }

            if (uiCheckHandler != null) {
                uiCheckHandler.removeCallbacksAndMessages(null);
                uiCheckHandler = null;
            }
        } catch (Exception e) {
            logger.error("onDestroyView error : {}", e.getMessage());

        }
    }
}