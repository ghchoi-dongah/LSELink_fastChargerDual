package com.dongah.fastcharger.pages;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

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
import com.dongah.fastcharger.utils.SharedModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
    TextView textViewTagTimer, textViewMemberCheckMessage;
    ImageView imageViewMemberCard;
    Animation animation;
    Handler countHandler;
    Runnable countRunnable;
    MainActivity activity;
    ChargingCurrentData chargingCurrentData;

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
        activity = (MainActivity) MainActivity.mContext;
        chargingCurrentData = activity.getChargingCurrentData(mChannel);

        textViewMemberCheckMessage = view.findViewById(R.id.textViewMemberCheckMessage);
        textViewTagTimer = view.findViewById(R.id.textViewTagTimer);
        imageViewMemberCard = view.findViewById(R.id.imageViewMemberCard);
        animation = AnimationUtils.loadAnimation(getContext(), R.anim.translate);
        String[] requestStrings = new String[1];
        SharedModel sharedModel = new ViewModelProvider(requireActivity()).get(SharedModel.class);
        requestStrings[0] = String.valueOf(mChannel);
        sharedModel.setMutableLiveData(requestStrings);

        // rfCard ready
        activity.getRfCardReaderReceive().rfCardReadRequest(mChannel);

        return view;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            textViewTagTimer.setText(timer + "초");
            updateCardBackground(chargingCurrentData.getAuthType());
            imageViewMemberCard.startAnimation(animation);

            countHandler = new Handler();
            countRunnable = new Runnable() {
                @Override
                public void run() {
                    timer--;
                    if (timer <= 0) {
                        countHandler.removeCallbacks(countRunnable);
                        ((MainActivity) MainActivity.mContext).getClassUiProcess(mChannel).onHome();
                    } else {
                        countHandler.postDelayed(countRunnable, 1000);
                        textViewTagTimer.setText(timer + "초");
                    }
                }
            };
            countHandler.postDelayed(countRunnable, 1000);
        } catch (Exception e) {
            logger.error("onViewCreated error: {}", e.getMessage(), e);
        }
    }

    private void updateCardBackground(String type) {
        switch (type) {
            case "C":
                imageViewMemberCard.setBackgroundResource(R.drawable.corp_card);
                textViewMemberCheckMessage.setText(getString(R.string.corpCardTagMessage));
                break;
            case "K":
                imageViewMemberCard.setBackgroundResource(R.drawable.moe_card);
                textViewMemberCheckMessage.setText(getString(R.string.moeCardTagMessage));
                break;
            default:
                imageViewMemberCard.setBackgroundResource(R.drawable.member_card);
                textViewMemberCheckMessage.setText(getString(R.string.memberCardTagMessage));
                break;
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
                imageViewMemberCard.clearAnimation();
                animation.setAnimationListener(null);
                animation = null;
            }
        } catch (Exception e) {
            logger.error("onDestroyView error : {}", e.getMessage(), e);
        }
        super.onDestroyView();
    }
}