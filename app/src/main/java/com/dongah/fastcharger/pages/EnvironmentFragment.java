package com.dongah.fastcharger.pages;

import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.UiSeq;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EnvironmentFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EnvironmentFragment extends Fragment implements View.OnClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    Button btnConfig, btnWebSocket, btnControl, btnDbControl, btnLoadTest, btnUi, btnSystemExit, btnRemoteTest;
    FragmentTransaction transaction;

    public EnvironmentFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EnvironmentFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EnvironmentFragment newInstance(String param1, String param2) {
        EnvironmentFragment fragment = new EnvironmentFragment();
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
        View view = inflater.inflate(R.layout.fragment_environment, container, false);
        btnConfig = view.findViewById(R.id.btnConfig);
        btnConfig.setOnClickListener(this);
        btnWebSocket = view.findViewById(R.id.btnWebSocket);
        btnWebSocket.setOnClickListener(this);
        btnControl = view.findViewById(R.id.btnControl);
        btnControl.setOnClickListener(this);
        btnDbControl = view.findViewById(R.id.btnDbControl);
        btnDbControl.setOnClickListener(this);
        btnLoadTest = view.findViewById(R.id.btnLoadTest);
        btnLoadTest.setOnClickListener(this);
        btnUi = view.findViewById(R.id.btnUi);
        btnUi.setOnClickListener(this);
        btnSystemExit = view.findViewById(R.id.btnSystemExit);
        btnSystemExit.setOnClickListener(this);
        btnRemoteTest = view.findViewById(R.id.btnRemoteTest);
        btnRemoteTest.setOnClickListener(this);
        return  view;
    }

    @Override
    public void onClick(View v) {
        int getId = v.getId();
        if (Objects.equals(getId, R.id.btnConfig)) {
            ((MainActivity) MainActivity.mContext).getFragmentChange().onFragmentChange(mChannel, UiSeq.CONFIG_SETTING, "CONFIG_SETTING", null);
        } else if (Objects.equals(getId, R.id.btnWebSocket)) {
            ((MainActivity) MainActivity.mContext).getFragmentChange().onFragmentChange(mChannel,UiSeq.WEB_SOCKET, "WEB_SOCKET", null);
        } else if (Objects.equals(getId, R.id.btnControl)) {
            ((MainActivity) MainActivity.mContext).getFragmentChange().onFragmentChange(mChannel,UiSeq.CONTROL_BOARD_DEBUGGING, "CONTROL_BOARD_DEBUGGING", null);
        } else if (Objects.equals(getId, R.id.btnDbControl)) {
            // TODO: database control fragment change
        } else if (Objects.equals(getId, R.id.btnLoadTest)) {
            transaction = ((MainActivity) MainActivity.mContext).getSupportFragmentManager().beginTransaction();
            ProductTestFragment productTestFragment = new ProductTestFragment();
            transaction.replace(R.id.frameFull, productTestFragment);
            transaction.commit();
        } else if (Objects.equals(getId, R.id.btnUi)) {
            UiSeq uiSeq = ((MainActivity) MainActivity.mContext).getClassUiProcess(mChannel).getUiSeq();
            switch (uiSeq) {
                case CHARGING:
                    ((MainActivity) MainActivity.mContext).getClassUiProcess(mChannel).setUiSeq(UiSeq.CHARGING);
                    ((MainActivity) MainActivity.mContext).getFragmentChange().onFragmentChange(mChannel,UiSeq.CHARGING, "CHARGING", null);
                    break;
                case FAULT:
                    ((MainActivity) MainActivity.mContext).getClassUiProcess(mChannel).setUiSeq(UiSeq.FAULT);
                    ((MainActivity) MainActivity.mContext).getFragmentChange().onFragmentChange(mChannel,UiSeq.FAULT, "FAULT", null);
                    break;
                default:
                    MainActivity activity = (MainActivity) MainActivity.mContext;
                    for (int ch = 0; ch < GlobalVariables.maxChannel; ch++) {
                        activity.getClassUiProcess(ch).setUiSeq(UiSeq.INIT);
                        ((MainActivity) MainActivity.mContext).getClassUiProcess(ch).onHome();
                    }
                    break;
            }
        } else if (Objects.equals(getId, R.id.btnSystemExit)) {
            ActivityCompat.finishAffinity((MainActivity) MainActivity.mContext);
            System.exit(0);
        } else if (Objects.equals(getId, R.id.btnRemoteTest)) {
            for (int i = 0; i < GlobalVariables.maxChannel; i++) {
                ((MainActivity) MainActivity.mContext).getClassUiProcess(i).setUiSeq(UiSeq.REMOTE_TEST);
            }
            ((MainActivity) MainActivity.mContext).getFragmentChange().onFragmentChange(mChannel,UiSeq.REMOTE_TEST, "REMOTE_TEST", null);
        }
    }
}