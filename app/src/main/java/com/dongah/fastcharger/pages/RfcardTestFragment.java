package com.dongah.fastcharger.pages;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.GlobalVariables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RfcardTestFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RfcardTestFragment extends Fragment {
    private static final Logger logger = LoggerFactory.getLogger(RfcardTestFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    TextView txtCardNum;
    Button btnExit, btnRfTest;

    public RfcardTestFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RfcardTestFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RfcardTestFragment newInstance(String param1, String param2) {
        RfcardTestFragment fragment = new RfcardTestFragment();
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
        View view = inflater.inflate(R.layout.fragment_rfcard_test, container, false);
        try {
            txtCardNum = view.findViewById(R.id.txtCardNum);
            btnExit = view.findViewById(R.id.btnExit);
            btnExit.setOnClickListener(v ->  exitFragment());
            btnRfTest = view.findViewById(R.id.btnRfTest);
            btnRfTest.setOnClickListener(v -> startRfcardTest());
        } catch (Exception e) {
            logger.error("onCreateView error : {}", e.getMessage(), e);
        }
        return view;
    }

    @SuppressLint("SetTextI18n")
    private void startRfcardTest() {
        GlobalVariables.rfcardTestMode = true;
        GlobalVariables.rfCardTestCallback = cardId -> {
            new Handler(Looper.getMainLooper()).post(() -> {
                txtCardNum.setText("카드번호: " + cardId);
            });
        };
        ((MainActivity) MainActivity.mContext).getRfCardReaderReceive().rfCardReadRequest(0);
    }

    private void exitFragment() {
        try {
            GlobalVariables.rfcardTestMode = false;
            GlobalVariables.rfCardTestCallback = null;
            FragmentTransaction transaction = ((MainActivity) MainActivity.mContext).getSupportFragmentManager().beginTransaction();
            EnvironmentFragment environmentFragment = new EnvironmentFragment();
            transaction.replace(R.id.frameFull, environmentFragment);
            transaction.commit();
        } catch (Exception e) {
            logger.error("onClick error: {}", e.getMessage());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        try {
            GlobalVariables.rfcardTestMode = false;
            GlobalVariables.rfCardTestCallback = null;
            ((MainActivity) MainActivity.mContext).getRfCardReaderReceive().rfCardReadRelease();
        } catch (Exception e) {
            logger.error("onDetach error : {}", e.getMessage(), e);
        }
    }
}