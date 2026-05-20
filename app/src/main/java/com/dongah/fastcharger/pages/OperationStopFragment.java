package com.dongah.fastcharger.pages;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import com.dongah.fastcharger.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OperationStopFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OperationStopFragment extends Fragment implements View.OnClickListener {
    private static final Logger logger = LoggerFactory.getLogger(OperationStopFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    ImageView imageViewOpStop;
    ObjectAnimator fadeAnimator;

    public OperationStopFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OperationStopFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OperationStopFragment newInstance(String param1, String param2) {
        OperationStopFragment fragment = new OperationStopFragment();
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
        View view = inflater.inflate(R.layout.fragment_operation_stop, container, false);
        imageViewOpStop = view.findViewById(R.id.imageViewOpStop);

        // imageViewOpStop animation
        fadeAnimator = ObjectAnimator.ofFloat(imageViewOpStop, "alpha", 1f, 0.3f);
        fadeAnimator.setDuration(1000);
        fadeAnimator.setRepeatCount(ValueAnimator.INFINITE);
        fadeAnimator.setRepeatMode(ValueAnimator.REVERSE);
        fadeAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeAnimator.start();

        return view;
    }

    @Override
    public void onClick(View v) {
        if (!isAdded()) return;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        try {
            if (fadeAnimator != null) {
                fadeAnimator.cancel();
                fadeAnimator = null;
            }
        } catch (Exception e) {
            logger.error("OperationStopFragment onDestroyView error : {}", e.getMessage());
        }
    }
}