package com.dongah.fastcharger.pages;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.UiSeq;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WebSocketDebugFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WebSocketDebugFragment extends Fragment implements View.OnClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    Button btnWebClose, btnWebClear;
    TextView textViewDebugMessage;

    public WebSocketDebugFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment WebSocketDebugFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static WebSocketDebugFragment newInstance(String param1, String param2) {
        WebSocketDebugFragment fragment = new WebSocketDebugFragment();
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
        View view = inflater.inflate(R.layout.fragment_web_socket_debug, container, false);
        btnWebClose = view.findViewById(R.id.btnWebClose);
        btnWebClose.setOnClickListener(this);
        btnWebClear = view.findViewById(R.id.btnWebClear);
        btnWebClear.setOnClickListener(this);
        textViewDebugMessage = view.findViewById(R.id.textViewDebugMessage);
        return view;
    }

    @Override
    public void onClick(View v) {
        int getId = v.getId();
        if (Objects.equals(getId, R.id.btnWebClose)) {
            ((MainActivity) MainActivity.mContext).getFragmentChange().onFragmentChange(mChannel, UiSeq.ENVIRONMENT, "ENVIRONMENT", null);
        } else if (Objects.equals(getId, R.id.btnWebClear)) {
            textViewDebugMessage.setText(null);
        }
    }
}