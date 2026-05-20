package com.dongah.fastcharger.pages;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.UiSeq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HeaderFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HeaderFragment extends Fragment implements View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger(HeaderFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    private final Handler handler = new Handler(Looper.getMainLooper());
    int clickedCnt = 0;
    ImageButton btnLogo;
    TextView textViewChargerId;
    ChargerConfiguration chargerConfiguration;

    public HeaderFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HeaderFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HeaderFragment newInstance(String param1, String param2) {
        HeaderFragment fragment = new HeaderFragment();
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

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_header, container, false);
        btnLogo = view.findViewById(R.id.btnLogo);
        btnLogo.setOnClickListener(this);
        textViewChargerId = view.findViewById(R.id.textViewChargerId);

        try {
            chargerConfiguration = ((MainActivity) MainActivity.mContext).getChargerConfiguration();
            textViewChargerId.setText("| ID-" + chargerConfiguration.getChargerId());
        } catch (Exception e) {
            logger.error("HeaderFragment onCreateView error : {}", e.getMessage());
        }
        return view;
    }

    @Override
    public void onClick(View v) {
        if (Objects.equals(v.getId(), R.id.btnLogo)) {
            System.out.println("btnLogo click: " + clickedCnt);
            if (clickedCnt > 8) {
                try {
                    MainActivity activity = (MainActivity) MainActivity.mContext;
                    if (activity == null) {
                        System.out.println("btnLogo error: MainActivity.mContext is null");
                        return;
                    }

                    UiSeq ui0 = activity.getClassUiProcess(0) != null
                            ? activity.getClassUiProcess(0).getUiSeq()
                            : null;
                    UiSeq ui1 = activity.getClassUiProcess(1) != null
                            ? activity.getClassUiProcess(1).getUiSeq()
                            : null;

                    boolean chkUiSeq = (ui0 == UiSeq.INIT || ui0 == UiSeq.SEQUENTIAL_CHARGING || ui0 == UiSeq.FAULT || ui0 == UiSeq.OP_STOP) &&
                                            (ui1 == UiSeq.INIT || ui1 == UiSeq.SEQUENTIAL_CHARGING || ui1 == UiSeq.FAULT || ui1 == UiSeq.OP_STOP);
                    System.out.println("clickedCnt > 8, ui0: " + ui0 + ", ui1: " + ui1 + ", chkUiSeq: " + chkUiSeq + ", mChannel:" + mChannel);
                    if (chkUiSeq) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                activity.getFragmentChange().onFragmentChange(mChannel, UiSeq.ADMIN_PASS,"ADMIN_PASS",null);
                            }
                        });
                    }
                    clickedCnt = 0;
                } catch (Exception e) {
                    logger.error("HeaderFragment btnLogo error : {}", e.getMessage());
                }
            }
            clickedCnt++;
        }
    }
}