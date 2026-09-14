package com.dongah.fastcharger.pages;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ToggleButton;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.controlboard.ControlBoard;
import com.dongah.fastcharger.controlboard.RxData;
import com.dongah.fastcharger.controlboard.TxData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProductTestIoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProductTestIoFragment extends Fragment implements View.OnClickListener {
    private static final Logger logger = LoggerFactory.getLogger(ProductTestIoFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    EditText editRy1, editRy2, editRy3, editRy4, editRy5, editRy6,
            editRy7, editRy8, editRy9, editRy10, editRy11, editRy12;
    ToggleButton btnRy1, btnRy2, btnRy3, btnRy4, btnRy5, btnRy6,
            btnRy7, btnRy8, btnRy9, btnRy10, btnRy11, btnRy12;

    ControlBoard controlBoard;
    RxData rxData1, rxData2;
    TxData txData1, txData2;

    public ProductTestIoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ProductTestIoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ProductTestIoFragment newInstance(String param1, String param2) {
        ProductTestIoFragment fragment = new ProductTestIoFragment();
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
        View view = inflater.inflate(R.layout.fragment_product_test_io, container, false);
        try {
            controlBoard = ((MainActivity) MainActivity.mContext).getControlBoard();
            rxData1 = controlBoard.getRxData(0);
            rxData2 = controlBoard.getRxData(1);
            txData1 = controlBoard.getTxData(0);
            txData2 = controlBoard.getTxData(1);

            editRy1 = view.findViewById(R.id.editRy1);
            editRy2 = view.findViewById(R.id.editRy2);
            editRy3 = view.findViewById(R.id.editRy3);
            editRy4 = view.findViewById(R.id.editRy4);
            editRy5 = view.findViewById(R.id.editRy5);
            editRy6 = view.findViewById(R.id.editRy6);
            editRy7 = view.findViewById(R.id.editRy7);
            editRy8 = view.findViewById(R.id.editRy8);
            editRy9 = view.findViewById(R.id.editRy9);
            editRy10 = view.findViewById(R.id.editRy10);
            editRy11 = view.findViewById(R.id.editRy11);
            editRy12 = view.findViewById(R.id.editRy12);

            // btnRy 초기화
            btnRy1  = view.findViewById(R.id.btnRy1);
            btnRy1.setOnClickListener(this);
            btnRy2  = view.findViewById(R.id.btnRy2);
            btnRy2.setOnClickListener(this);
            btnRy3  = view.findViewById(R.id.btnRy3);
            btnRy3.setOnClickListener(this);
            btnRy4  = view.findViewById(R.id.btnRy4);
            btnRy4.setOnClickListener(this);
            btnRy5  = view.findViewById(R.id.btnRy5);
            btnRy5.setOnClickListener(this);
            btnRy6  = view.findViewById(R.id.btnRy6);
            btnRy6.setOnClickListener(this);
            btnRy7  = view.findViewById(R.id.btnRy7);
            btnRy7.setOnClickListener(this);
            btnRy8  = view.findViewById(R.id.btnRy8);
            btnRy8.setOnClickListener(this);
            btnRy9  = view.findViewById(R.id.btnRy9);
            btnRy9.setOnClickListener(this);
            btnRy10 = view.findViewById(R.id.btnRy10);
            btnRy10.setOnClickListener(this);
            btnRy11 = view.findViewById(R.id.btnRy11);
            btnRy11.setOnClickListener(this);
            btnRy12 = view.findViewById(R.id.btnRy12);
            btnRy12.setOnClickListener(this);
        } catch (Exception e) {
            logger.error("onCreateView error : {}", e.getMessage(), e);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            for (int i = 0; i < GlobalVariables.maxChannel; i++) {
                controlBoard.getTxData(i).setChargerPointMode((short) 2);
            }

            // btnRy 초기값 설정
            btnRy1.setChecked(txData1.isRelay1());
            btnRy2.setChecked(txData1.isRelay2());
            btnRy3.setChecked(txData1.isRelay3());
            btnRy4.setChecked(txData1.isRelay4());
            btnRy5.setChecked(txData1.isRelay5());
            btnRy6.setChecked(txData1.isRelay6());
            btnRy7.setChecked(txData2.isRelay1());
            btnRy8.setChecked(txData2.isRelay2());
            btnRy9.setChecked(txData2.isRelay3());
            btnRy10.setChecked(txData2.isRelay4());
            btnRy11.setChecked(txData2.isRelay5());
            btnRy12.setChecked(txData2.isRelay6());

            editRy1.setText(rxData1.isCsRY1Status() ? "ON" : "OFF");
            editRy2.setText(rxData1.isCsRY2Status() ? "ON" : "OFF");
            editRy3.setText(rxData1.isCsRY3Status() ? "ON" : "OFF");
            editRy4.setText(rxData1.isCsRY4Status() ? "ON" : "OFF");
            editRy5.setText(rxData1.isCsRY5Status() ? "ON" : "OFF");
            editRy6.setText(rxData1.isCsRY6Status() ? "ON" : "OFF");
            editRy7.setText(rxData2.isCsRY1Status() ? "ON" : "OFF");
            editRy8.setText(rxData2.isCsRY2Status() ? "ON" : "OFF");
            editRy9.setText(rxData2.isCsRY3Status() ? "ON" : "OFF");
            editRy10.setText(rxData2.isCsRY4Status() ? "ON" : "OFF");
            editRy11.setText(rxData2.isCsRY5Status() ? "ON" : "OFF");
            editRy12.setText(rxData2.isCsRY6Status() ? "ON" : "OFF");
        } catch (Exception e) {
            logger.error("onViewCreated error : {}", e.getMessage(), e);
        }
    }

    @Override
    public void onClick(View v) {
        int getId = v.getId();
        try {
            if (Objects.equals(getId, R.id.btnRy1)) {
                txData1.setRelay1(btnRy1.isChecked());
                editRy1.setText(rxData1.isCsRY1Status() ? "ON" : "OFF");
            } else if (Objects.equals(getId, R.id.btnRy2)) {
                txData1.setRelay2(btnRy2.isChecked());
                editRy2.setText(rxData1.isCsRY2Status() ? "ON" : "OFF");
            } else if (Objects.equals(getId, R.id.btnRy3)) {
                txData1.setRelay3(btnRy3.isChecked());
                editRy3.setText(rxData1.isCsRY3Status() ? "ON" : "OFF");
            } else if (Objects.equals(getId, R.id.btnRy4)) {
                txData1.setRelay4(btnRy4.isChecked());
                editRy4.setText(rxData1.isCsRY4Status() ? "ON" : "OFF");
            } else if (Objects.equals(getId, R.id.btnRy5)) {
                txData1.setRelay5(btnRy5.isChecked());
                editRy5.setText(rxData1.isCsRY5Status() ? "ON" : "OFF");
            } else if (Objects.equals(getId, R.id.btnRy6)) {
                txData1.setRelay6(btnRy6.isChecked());
                editRy6.setText(rxData1.isCsRY6Status() ? "ON" : "OFF");
            } else if (Objects.equals(getId, R.id.btnRy7)) {
                txData2.setRelay1(btnRy7.isChecked());
                editRy7.setText(rxData2.isCsRY1Status() ? "ON" : "OFF");
            } else if (Objects.equals(getId, R.id.btnRy8)) {
                txData2.setRelay2(btnRy8.isChecked());
                editRy8.setText(rxData2.isCsRY2Status() ? "ON" : "OFF");
            } else if (Objects.equals(getId, R.id.btnRy9)) {
                txData2.setRelay3(btnRy9.isChecked());
                editRy9.setText(rxData2.isCsRY3Status() ? "ON" : "OFF");
            } else if (Objects.equals(getId, R.id.btnRy10)) {
                txData2.setRelay4(btnRy10.isChecked());
                editRy10.setText(rxData2.isCsRY4Status() ? "ON" : "OFF");
            } else if (Objects.equals(getId, R.id.btnRy11)) {
                txData2.setRelay5(btnRy11.isChecked());
                editRy11.setText(rxData2.isCsRY5Status() ? "ON" : "OFF");
            } else if (Objects.equals(getId, R.id.btnRy12)) {
                txData2.setRelay6(btnRy12.isChecked());
                editRy12.setText(rxData2.isCsRY6Status() ? "ON" : "OFF");
            }
        } catch (Exception e) {
            logger.error("onClick error : {}", e.getMessage(), e);
        }
    }
}