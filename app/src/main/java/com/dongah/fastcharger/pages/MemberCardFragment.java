package com.dongah.fastcharger.pages;

import android.annotation.SuppressLint;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.utils.SharedModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MemberCardFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MemberCardFragment extends Fragment {

    private static final Logger logger = LoggerFactory.getLogger(MemberCardFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    int timer = 20;
    TextView textViewTagTimer, textViewMessage;
    ImageView imageViewMemberCard;
    AnimationDrawable animationDrawable;
    ChargingCurrentData chargingCurrentData;
    Handler countHandler;
    Runnable countRunnable;

    public MemberCardFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MemberCardFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MemberCardFragment newInstance(String param1, String param2) {
        MemberCardFragment fragment = new MemberCardFragment();
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
        View view = inflater.inflate(R.layout.fragment_member_card, container, false);
        textViewTagTimer = view.findViewById(R.id.textViewTagTimer);
        textViewMessage = view.findViewById(R.id.textViewMessage);
        imageViewMemberCard = view.findViewById(R.id.imageViewMemberCard);
        imageViewMemberCard.setBackgroundResource(R.drawable.membercardtagging);
        animationDrawable = (AnimationDrawable) imageViewMemberCard.getBackground();
        chargingCurrentData = ((MainActivity) MainActivity.mContext).getChargingCurrentData(mChannel);
        String[] requestStrings = new String[1];
        SharedModel sharedModel = new ViewModelProvider(requireActivity()).get(SharedModel.class);
        requestStrings[0] = String.valueOf(mChannel);
        sharedModel.setMutableLiveData(requestStrings);

        textViewMessage.setVisibility(Objects.equals(chargingCurrentData.getAuthType(), "M") ? View.VISIBLE : View.INVISIBLE);
        chargingCurrentData.setAuthType("C");

        // rfCard ready
        ((MainActivity) MainActivity.mContext).getRfCardReaderReceive().rfCardReadRequest(mChannel);

        return view;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            animationDrawable.start();
            textViewTagTimer.setText(timer + "초");

            countHandler = new Handler();
            countRunnable = new Runnable() {
                @Override
                public void run() {
                    timer--;
                    if (timer <= 0) {
                        ((MainActivity) MainActivity.mContext).getClassUiProcess(mChannel).onHome();
                    } else {
                        countHandler.postDelayed(countRunnable, 1000);
                        textViewTagTimer.setText(timer + "초");
                    }
                }
            };
            countHandler.postDelayed(countRunnable, 1000);
        } catch (Exception e) {
            logger.error("MemberCardFragment error: {}", e.getMessage());
        }
    }

    @Override
    public void onDestroyView() {
        try {
            if (animationDrawable != null) {
                animationDrawable.stop();
            }

            if (imageViewMemberCard != null) {
                Drawable bg = imageViewMemberCard.getBackground();
                if (bg instanceof AnimationDrawable) {
                    ((AnimationDrawable) bg).stop();
                }
                imageViewMemberCard.setBackground(null);
            }
        } catch (Exception e) {
            logger.error("MemberCardFragment onDestroyView : {}", e.getMessage());
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
            logger.error("MemberCardFragment onDetach : {}", e.getMessage());
        }
    }
}