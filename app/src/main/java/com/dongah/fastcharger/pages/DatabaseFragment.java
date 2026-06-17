package com.dongah.fastcharger.pages;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.sqlite.SQLiteHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DatabaseFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DatabaseFragment extends Fragment implements View.OnClickListener {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private static final String[] TABLES = {
            "CP_UNIT_PRICE", "CP_CHANGE_MODE", "CP_CHG_ELECMODE", "CP_RECHG_SOC"
    };

    private static final int COL_MIN_WIDTH_DP = 160;
    private static final int CELL_PADDING_DP  = 8;

    private Spinner spinnerTable;
    private Button btnQuery;
    private Button            btnClose;
    private TextView tvRowCount;
    private TextView          tvEmpty;
    private HorizontalScrollView scrollH;
    private LinearLayout headerRow;
    private LinearLayout      dataContainer;

    public DatabaseFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment DatabaseFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DatabaseFragment newInstance(String param1, String param2) {
        DatabaseFragment fragment = new DatabaseFragment();
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
        View view = inflater.inflate(R.layout.fragment_database, container, false);

        spinnerTable  = view.findViewById(R.id.spinnerTable);
        btnQuery      = view.findViewById(R.id.btnQuery);
        btnClose      = view.findViewById(R.id.btnClose);
        tvRowCount    = view.findViewById(R.id.tvRowCount);
        tvEmpty       = view.findViewById(R.id.tvEmpty);
        scrollH       = view.findViewById(R.id.scrollH);
        headerRow     = view.findViewById(R.id.headerRow);
        dataContainer = view.findViewById(R.id.dataContainer);

        setupSpinner();
        btnQuery.setOnClickListener(this);
        btnClose.setOnClickListener(this);

        return view;
    }

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                TABLES
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTable.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        if (Objects.equals(v.getId(), R.id.btnQuery)) {
            loadTable((String) spinnerTable.getSelectedItem());
        } else if (Objects.equals(v.getId(), R.id.btnClose)) {
            FragmentTransaction tx = ((MainActivity) MainActivity.mContext)
                    .getSupportFragmentManager().beginTransaction();
            tx.replace(R.id.frameFull, new EnvironmentFragment());
            tx.commit();
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadTable(String tableName) {
        headerRow.removeAllViews();
        dataContainer.removeAllViews();

        SQLiteHelper helper = SQLiteHelper.getInstance(requireContext());
        Cursor cursor = null;
        try {
            cursor = helper.selectAll(tableName);
            if (cursor == null) {
                showEmpty("데이터 없음");
                return;
            }

            String[] cols = cursor.getColumnNames();
            int colCount  = cols.length;
            int colWidth  = dp(COL_MIN_WIDTH_DP);
            int cellPad   = dp(CELL_PADDING_DP);

            // 헤더 행 구성
            for (String col : cols) {
                headerRow.addView(makeCell(col, colWidth, cellPad, true, 0));
                headerRow.addView(makeDivider());
            }

            // 데이터 행 구성
            int rowIndex = 0;
            while (cursor.moveToNext()) {
                LinearLayout row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setBackgroundColor(rowIndex % 2 == 0
                        ? Color.parseColor("#121212") : Color.parseColor("#1a1a1a"));
                for (int i = 0; i < colCount; i++) {
                    String val = cursor.isNull(i) ? "—" : cursor.getString(i);
                    row.addView(makeCell(val, colWidth, cellPad, false, i));
                    row.addView(makeDivider());
                }
                dataContainer.addView(row);
                rowIndex++;
            }

            tvRowCount.setText(rowIndex + " rows");
            tvEmpty.setVisibility(View.GONE);
            scrollH.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            logger.error("loadTable error: {}", e.getMessage());
            showEmpty("오류: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private TextView makeCell(String text, int minWidth, int padding,
                              boolean isHeader, int colIndex) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setMinWidth(minWidth);
        tv.setPadding(padding, padding, padding, padding);
        tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        tv.setSingleLine(true);

        if (isHeader) {
            tv.setTextColor(Color.parseColor("#bb86fc"));
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextSize(13f);
        } else {
            tv.setTextColor(colIndex == 0
                    ? Color.parseColor("#aaaaaa")   // ID 컬럼은 흐리게
                    : Color.parseColor("#e0e0e0"));
            tv.setTypeface(Typeface.MONOSPACE);
            tv.setTextSize(12f);
        }

        return tv;
    }

    private View makeDivider() {
        View v = new View(requireContext());
        v.setLayoutParams(new LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
        v.setBackgroundColor(Color.parseColor("#333333"));
        return v;
    }

    private void showEmpty(String msg) {
        tvEmpty.setText(msg);
        tvEmpty.setVisibility(View.VISIBLE);
        scrollH.setVisibility(View.GONE);
        tvRowCount.setText("");
    }

    private int dp(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}