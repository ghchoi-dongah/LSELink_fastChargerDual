package com.dongah.fastcharger.pages;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.utils.FileManagement;
import com.dongah.fastcharger.websocket.ocpp.core.Reason;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link RemoteTestFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RemoteTestFragment extends Fragment implements View.OnClickListener {
    private static final Logger logger = LoggerFactory.getLogger(RemoteTestFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String CHANNEL = "CHANNEL";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private int mChannel;

    Button btnResetHard, btnResetSoft, btnInoperative, btnOperative, btnInoperativeAll, btnOperativeAll;
    Button btnTestChangeModeDM, btnTestChangeModeIM, btnTestChangeElecMode, btnRechgrsocschedule;
    Button btnTestChangeElecMode2, btnRechgrsocschedule2, btnTestChangeModeDMCh0, btnTestChangeModeIMCh0;
    Button btnTestWebsocketUrl;
    Button btnExit;

    MainActivity activity;
    ChargingCurrentData chargingCurrentData;


    public RemoteTestFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment RemoteTestFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static RemoteTestFragment newInstance(String param1, String param2) {
        RemoteTestFragment fragment = new RemoteTestFragment();
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

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_remote_test, container, false);
        activity = (MainActivity) MainActivity.mContext;
        chargingCurrentData = activity.getChargingCurrentData(mChannel);

        btnExit = view.findViewById(R.id.btnExit);
        btnExit.setOnClickListener(this);
        btnResetHard = view.findViewById(R.id.btnResetHard);
        btnResetHard.setOnClickListener(this);
        btnResetSoft = view.findViewById(R.id.btnResetSoft);
        btnResetSoft.setOnClickListener(this);
        btnInoperative = view.findViewById(R.id.btnInoperative);
        btnInoperative.setOnClickListener(this);
        btnOperative = view.findViewById(R.id.btnOperative);
        btnOperative.setOnClickListener(this);
        btnInoperativeAll = view.findViewById(R.id.btnInoperativeAll);
        btnInoperativeAll.setOnClickListener(this);
        btnOperativeAll = view.findViewById(R.id.btnOperativeAll);
        btnOperativeAll.setOnClickListener(this);

        btnTestChangeModeDM = view.findViewById(R.id.btnTestChangeModeDM);
        btnTestChangeModeDM.setOnClickListener(this);
        btnTestChangeModeIM = view.findViewById(R.id.btnTestChangeModeIM);
        btnTestChangeModeIM.setOnClickListener(this);
        btnTestChangeElecMode = view.findViewById(R.id.btnTestChangeElecMode);
        btnTestChangeElecMode.setOnClickListener(this);
        btnRechgrsocschedule = view.findViewById(R.id.btnRechgrsocschedule);
        btnRechgrsocschedule.setOnClickListener(this);
        btnTestChangeElecMode2 = view.findViewById(R.id.btnTestChangeElecMode2);
        btnTestChangeElecMode2.setOnClickListener(this);
        btnRechgrsocschedule2 = view.findViewById(R.id.btnRechgrsocschedule2);
        btnRechgrsocschedule2.setOnClickListener(this);

        btnTestChangeModeDMCh0 = view.findViewById(R.id.btnTestChangeModeDMCh0);
        btnTestChangeModeDMCh0.setOnClickListener(this);
        btnTestChangeModeIMCh0 = view.findViewById(R.id.btnTestChangeModeIMCh0);
        btnTestChangeModeIMCh0.setOnClickListener(this);
        btnTestWebsocketUrl = view.findViewById(R.id.btnTestWebsocketUrl);
        btnTestWebsocketUrl.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        int getId = v.getId();
        if (Objects.equals(getId, R.id.btnExit)) {
            FragmentTransaction transaction = ((MainActivity) MainActivity.mContext).getSupportFragmentManager().beginTransaction();
            EnvironmentFragment environmentFragment = new EnvironmentFragment();
            transaction.replace(R.id.frameFull, environmentFragment);
            transaction.commit();
        } else if (Objects.equals(getId, R.id.btnResetHard) || Objects.equals(getId, R.id.btnResetSoft)) {
            for (int i = 0; i < GlobalVariables.maxChannel; i++) {
                activity.getChargingCurrentData(i).setStopReason(Objects.equals(getId, R.id.btnResetHard) ? Reason.HardReset : Reason.SoftReset);
                activity.getChargingCurrentData(i).setReBoot(true);
            }
        } else if (Objects.equals(getId, R.id.btnInoperative) || Objects.equals(getId, R.id.btnOperative)) {
            String type = Objects.equals(getId, R.id.btnInoperative) ? "Inoperative" : "Operative";
            onTestChangeAvailability(1, type);
        } else if (Objects.equals(getId, R.id.btnInoperativeAll) || Objects.equals(getId, R.id.btnOperativeAll)) {
            String type = Objects.equals(getId, R.id.btnInoperativeAll) ? "Inoperative" : "Operative";
            onTestChangeAvailability(0, type);
        } else if (Objects.equals(getId, R.id.btnTestChangeModeDM) || Objects.equals(getId, R.id.btnTestChangeModeIM)) {
            String type = Objects.equals(getId, R.id.btnTestChangeModeDM) ? "DM" : "NM";
            onTestChangeModeCommon(type, 0, 95, 60, "changemode0.req");
//            onTestChangeMode(type);
        } else if (Objects.equals(getId, R.id.btnTestChangeElecMode)) {
            onTestChangeElecModeCommon(1, "40", "70", "changeelecmode1.req");
//            onTestChangeElecMode();
        } else if (Objects.equals(getId, R.id.btnRechgrsocschedule)) {
            onTestRechgrsocscheduleCommon(1, "80", "rechgrsocschedule1.req");
//            onTestRechgrsocschedule();
        } else if (Objects.equals(getId, R.id.btnTestChangeElecMode2)) {
            onTestChangeElecModeCommon(2, "80", "80", "changeelecmode2.req");
//            onTestChangeElecMode2();
        } else if (Objects.equals(getId, R.id.btnRechgrsocschedule2)) {
            onTestRechgrsocscheduleCommon(2, "90", "rechgrsocschedule2.req");
//            onTestRechgrsocschedule2();
        } else if (Objects.equals(getId, R.id.btnTestChangeModeDMCh0) || Objects.equals(getId, R.id.btnTestChangeModeIMCh0)) {
            String type = Objects.equals(getId, R.id.btnTestChangeModeDMCh0) ? "DM" : "IM";
            onTestChangeModeCommon(type, 2, 70, 75, "changemode1.req");
//            onTestChangeMode2(type);
        } else if (Objects.equals(getId, R.id.btnTestWebsocketUrl)) {
            onTestWebSocketUrl();
        }
    }

    // save ChargerOperate
    private void onChargerOperateSave() {
        try {
            boolean chk;
            FileManagement fileManagement = new FileManagement();
            String rootPath = Environment.getExternalStorageDirectory().toString() + File.separator + "Download";
            String fileName = "ChargerOperate";
            File file = new File(rootPath + File.separator + fileName);
            if (file.exists()) chk = file.delete();

            for (int i = 0; i < GlobalVariables.maxPlugCount; i++) {
                String statusContent = String.valueOf(GlobalVariables.ChargerOperation[i]);
                fileManagement.stringToFileSave(rootPath, fileName, statusContent, true);
            }
        } catch (Exception e) {
            logger.error("onChargerOperateSave {}", e.getMessage());
        }
    }

    /**
     * ChangeAvailability 전문 시뮬레이션 테스트
     * CSMS에서 수신한 것과 동일한 전문을 onGetMessage에 직접 주입
     * @param type "Inoperative" 또는 "Operative"
     */
    private void onTestChangeAvailability(int connectorId, String type) {
        try {
            int loop = connectorId == 0 ? 2 : connectorId;

            for (int i = 0; i < loop; i++) {
                String uuid = UUID.randomUUID().toString();
                String testMessage = "[2,\"" + uuid + "\",\"ChangeAvailability\","
                        + "{\"connectorId\":" + i + ",\"type\":\"" + type + "\"}]";

                logger.info("[TEST] ChangeAvailability[{}]({}) 전문 주입: {}", i, type, testMessage);
                activity.getSocketReceiveMessage().onGetMessage(null, testMessage);
                logger.info("[TEST] ChangeAvailability[{}]({}) 처리 완료", i, type);
            }
        } catch (Exception e) {
            logger.error("[TEST] ChangeAvailability({}) 테스트 실패: {}", type, e.getMessage(), e);
        }
    }

    /**
     * DataTransfer changemode.req 전문 시뮬레이션 테스트
     * CSMS에서 수신한 것과 동일한 전문을 onGetMessage에 직접 주입
     */
    private void onTestChangeMode(String type) {
        try {
            String uuid = UUID.randomUUID().toString();
            String dataJson = "{\"connectorId\":0,\"rechgAmt\":95,\"rechgElec\":50,"
                    + "\"HH00\":\"" + type + "\",\"HH01\":\"" + type + "\",\"HH02\":\"" + type + "\",\"HH03\":\"" + type + "\","
                    + "\"HH04\":\"" + type + "\",\"HH05\":\"" + type + "\",\"HH06\":\"" + type + "\",\"HH07\":\"" + type + "\","
                    + "\"HH08\":\"" + type + "\",\"HH09\":\"" + type + "\",\"HH10\":\"" + type + "\",\"HH11\":\"" + type + "\","
                    + "\"HH12\":\"" + type + "\",\"HH13\":\"" + type + "\",\"HH14\":\"" + type + "\",\"HH15\":\"" + type + "\","
                    + "\"HH16\":\"" + type + "\",\"HH17\":\"" + type + "\",\"HH18\":\"" + type + "\",\"HH19\":\"" + type + "\","
                    + "\"HH20\":\"" + type + "\",\"HH21\":\"" + type + "\",\"HH22\":\"" + type + "\",\"HH23\":\"" + type + "\"}";

            String testMessage = "[2,\"" + uuid + "\",\"DataTransfer\","
                    + "{\"vendorId\":\"DONGAH\",\"messageId\":\"changemode.req\","
                    + "\"data\":\"" + dataJson.replace("\"", "\\\"") + "\"}]";

            logger.info("[TEST] changemode.req 전문 주입: {}", testMessage);
            activity.getSocketReceiveMessage().onGetMessage(null, testMessage);
            logger.info("[TEST] changemode.req 처리 완료");
        } catch (Exception e) {
            logger.error("[TEST] changemode.req 테스트 실패: {}", e.getMessage(), e);
        }
    }

    private void onTestChangeMode2(String type) {
        try {
            String uuid = UUID.randomUUID().toString();
            String dataJson = "{\"connectorId\":1,\"rechgAmt\":85,\"rechgElec\":55,"
                    + "\"HH00\":\"" + type + "\",\"HH01\":\"" + type + "\",\"HH02\":\"" + type + "\",\"HH03\":\"" + type + "\","
                    + "\"HH04\":\"" + type + "\",\"HH05\":\"" + type + "\",\"HH06\":\"" + type + "\",\"HH07\":\"" + type + "\","
                    + "\"HH08\":\"" + type + "\",\"HH09\":\"" + type + "\",\"HH10\":\"" + type + "\",\"HH11\":\"" + type + "\","
                    + "\"HH12\":\"" + type + "\",\"HH13\":\"" + type + "\",\"HH14\":\"" + type + "\",\"HH15\":\"" + type + "\","
                    + "\"HH16\":\"" + type + "\",\"HH17\":\"" + type + "\",\"HH18\":\"" + type + "\",\"HH19\":\"" + type + "\","
                    + "\"HH20\":\"" + type + "\",\"HH21\":\"" + type + "\",\"HH22\":\"" + type + "\",\"HH23\":\"" + type + "\"}";

            String testMessage = "[2,\"" + uuid + "\",\"DataTransfer\","
                    + "{\"vendorId\":\"DONGAH\",\"messageId\":\"changemode.req\","
                    + "\"data\":\"" + dataJson.replace("\"", "\\\"") + "\"}]";

            logger.info("[TEST] changemode2.req 전문 주입: {}", testMessage);
            activity.getSocketReceiveMessage().onGetMessage(null, testMessage);
            logger.info("[TEST] changemode2.req 처리 완료");
        } catch (Exception e) {
            logger.error("[TEST] changemode2.req 테스트 실패: {}", e.getMessage(), e);
        }
    }

    private void onTestChangeModeCommon(String type, int connectorId, int rechgAmt, int rechgElec, String logName) {
        try {
            String uuid = UUID.randomUUID().toString();

            StringBuilder dataJsonBuilder = new StringBuilder();
            dataJsonBuilder.append("{")
                    .append("\"connectorId\":").append(connectorId).append(",")
                    .append("\"rechgAmt\":").append(rechgAmt).append(",")
                    .append("\"rechgElec\":").append(rechgElec);

            for (int hour = 0; hour < 24; hour++) {
                dataJsonBuilder.append(",")
                        .append("\"HH")
                        .append(String.format("%02d", hour))
                        .append("\":\"")
                        .append(type)
                        .append("\"");
            }

            dataJsonBuilder.append("}");

            String dataJson = dataJsonBuilder.toString();

            String testMessage = "[2,\"" + uuid + "\",\"DataTransfer\","
                    + "{\"vendorId\":\"DONGAH\",\"messageId\":\"changemode.req\","
                    + "\"data\":\"" + dataJson.replace("\"", "\\\"") + "\"}]";

            logger.info("[TEST] {} 전문 주입: {}", logName, testMessage);

            activity.getSocketReceiveMessage().onGetMessage(null, testMessage);

            logger.info("[TEST] {} 처리 완료", logName);
        } catch (Exception e) {
            logger.error("[TEST] {} 테스트 실패: {}", logName, e.getMessage(), e);
        }
    }

    /**
     * DataTransfer changeelecmode.req 전문 시뮬레이션 테스트
     * CSMS에서 수신한 것과 동일한 전문을 onGetMessage에 직접 주입
     * connectorId=1, HH00~HH23 모두 "40"
     */
    private void onTestChangeElecMode() {
        try {
            String uuid = UUID.randomUUID().toString();
            String dataJson = "{\"connectorId\":1"
                    + ",\"HH00\":\"40\",\"HH01\":\"40\",\"HH02\":\"40\",\"HH03\":\"40\""
                    + ",\"HH04\":\"40\",\"HH05\":\"40\",\"HH06\":\"40\",\"HH07\":\"40\""
                    + ",\"HH08\":\"40\",\"HH09\":\"40\",\"HH10\":\"40\",\"HH11\":\"40\""
                    + ",\"HH12\":\"40\",\"HH13\":\"40\",\"HH14\":\"40\",\"HH15\":\"40\""
                    + ",\"HH16\":\"40\",\"HH17\":\"40\",\"HH18\":\"40\",\"HH19\":\"40\""
                    + ",\"HH20\":\"40\",\"HH21\":\"40\",\"HH22\":\"40\",\"HH23\":\"40\"}";

            String testMessage = "[2,\"" + uuid + "\",\"DataTransfer\","
                    + "{\"vendorId\":\"DONGAH\",\"messageId\":\"changeelecmode.req\","
                    + "\"data\":\"" + dataJson.replace("\"", "\\\"") + "\"}]";

            logger.info("[TEST] changeelecmode.req 전문 주입: {}", testMessage);
            activity.getSocketReceiveMessage().onGetMessage(null, testMessage);
            logger.info("[TEST] changeelecmode.req 처리 완료");
        } catch (Exception e) {
            logger.error("[TEST] changeelecmode.req 테스트 실패: {}", e.getMessage(), e);
        }
    }

    private void onTestChangeElecMode2() {
        try {
            String uuid = UUID.randomUUID().toString();
            String dataJson = "{\"connectorId\":2"
                    + ",\"HH00\":\"80\",\"HH01\":\"80\",\"HH02\":\"80\",\"HH03\":\"80\""
                    + ",\"HH04\":\"80\",\"HH05\":\"80\",\"HH06\":\"80\",\"HH07\":\"80\""
                    + ",\"HH08\":\"80\",\"HH09\":\"80\",\"HH10\":\"80\",\"HH11\":\"80\""
                    + ",\"HH12\":\"80\",\"HH13\":\"80\",\"HH14\":\"80\",\"HH15\":\"80\""
                    + ",\"HH16\":\"80\",\"HH17\":\"80\",\"HH18\":\"80\",\"HH19\":\"80\""
                    + ",\"HH20\":\"80\",\"HH21\":\"80\",\"HH22\":\"80\",\"HH23\":\"80\"}";

            String testMessage = "[2,\"" + uuid + "\",\"DataTransfer\","
                    + "{\"vendorId\":\"DONGAH\",\"messageId\":\"changeelecmode.req\","
                    + "\"data\":\"" + dataJson.replace("\"", "\\\"") + "\"}]";

            logger.info("[TEST] changeelecmode2.req 전문 주입: {}", testMessage);
            activity.getSocketReceiveMessage().onGetMessage(null, testMessage);
            logger.info("[TEST] changeelecmode2.req 처리 완료");
        } catch (Exception e) {
            logger.error("[TEST] changeelecmode2.req 테스트 실패: {}", e.getMessage(), e);
        }
    }

    private void onTestChangeElecModeCommon(int connectorId, String elecValue, String elecValue2, String logName) {
        try {
            String uuid = UUID.randomUUID().toString();

            StringBuilder dataJsonBuilder = new StringBuilder();
            dataJsonBuilder.append("{")
                    .append("\"connectorId\":").append(connectorId);

            for (int hour = 0; hour < 24; hour++) {


                if (hour == 15 || hour == 16) {
                    dataJsonBuilder.append(",")
                            .append("\"HH")
                            .append(String.format("%02d", hour))
                            .append("\":\"")
                            .append(elecValue2)
                            .append("\"");
                } else {
                    dataJsonBuilder.append(",")
                            .append("\"HH")
                            .append(String.format("%02d", hour))
                            .append("\":\"")
                            .append(elecValue)
                            .append("\"");
                }
            }

            dataJsonBuilder.append("}");

            String dataJson = dataJsonBuilder.toString();

            String testMessage = "[2,\"" + uuid + "\",\"DataTransfer\","
                    + "{\"vendorId\":\"DONGAH\",\"messageId\":\"changeelecmode.req\","
                    + "\"data\":\"" + dataJson.replace("\"", "\\\"") + "\"}]";

            logger.info("[TEST] {} 전문 주입: {}", logName, testMessage);

            activity.getSocketReceiveMessage().onGetMessage(null, testMessage);

            logger.info("[TEST] {} 처리 완료", logName);
        } catch (Exception e) {
            logger.error("[TEST] {} 테스트 실패: {}", logName, e.getMessage(), e);
        }
    }

    private void onTestRechgrsocschedule() {
        try {
            String uuid = UUID.randomUUID().toString();
            String dataJson = "{\"connectorId\":1"
                    + ",\"DH00\":\"80\",\"DH01\":\"80\",\"DH02\":\"80\",\"DH03\":\"80\",\"DH04\":\"80\",\"DH05\":\"80\""
                    + ",\"DH06\":\"80\",\"DH07\":\"80\",\"DH08\":\"80\",\"DH09\":\"80\",\"DH10\":\"80\",\"DH11\":\"80\""
                    + ",\"DH12\":\"80\",\"DH13\":\"80\",\"DH14\":\"80\",\"DH15\":\"80\",\"DH16\":\"80\",\"DH17\":\"80\""
                    + ",\"DH18\":\"80\",\"DH19\":\"80\",\"DH20\":\"80\",\"DH21\":\"80\",\"DH22\":\"80\",\"DH23\":\"80\""
                    + ",\"WH00\":\"80\",\"WH01\":\"80\",\"WH02\":\"80\",\"WH03\":\"80\",\"WH04\":\"80\",\"WH05\":\"80\""
                    + ",\"WH06\":\"80\",\"WH07\":\"80\",\"WH08\":\"80\",\"WH09\":\"80\",\"WH10\":\"80\",\"WH11\":\"80\""
                    + ",\"WH12\":\"80\",\"WH13\":\"80\",\"WH14\":\"80\",\"WH15\":\"80\",\"WH16\":\"80\",\"WH17\":\"80\""
                    + ",\"WH18\":\"80\",\"WH19\":\"80\",\"WH20\":\"80\",\"WH21\":\"80\",\"WH22\":\"80\",\"WH23\":\"80\""
                    + "}";

            String testMessage = "[2,\"" + uuid + "\",\"DataTransfer\","
                    + "{\"vendorId\":\"DONGAH\","
                    + "\"messageId\":\"rechgrsocschedule.req\","
                    + "\"data\":\"" + dataJson.replace("\"", "\\\"") + "\"}]";

            logger.info("[TEST] rechgrsocschedule2.req 전문 주입: {}", testMessage);
            activity.getSocketReceiveMessage().onGetMessage(null, testMessage);
            logger.info("[TEST] rechgrsocschedule2.req 처리 완료");
        } catch (Exception e) {
            logger.error("[TEST] rechgrsocschedule2.req 테스트 실패: {}", e.getMessage(), e);
        }
    }

    private void onTestRechgrsocschedule2() {
        try {
            String uuid = UUID.randomUUID().toString();
            String dataJson = "{\"connectorId\":2"
                    + ",\"DH00\":\"90\",\"DH01\":\"90\",\"DH02\":\"90\",\"DH03\":\"90\",\"DH04\":\"90\",\"DH05\":\"90\""
                    + ",\"DH06\":\"90\",\"DH07\":\"90\",\"DH08\":\"90\",\"DH09\":\"90\",\"DH10\":\"90\",\"DH11\":\"90\""
                    + ",\"DH12\":\"90\",\"DH13\":\"90\",\"DH14\":\"90\",\"DH15\":\"90\",\"DH16\":\"90\",\"DH17\":\"90\""
                    + ",\"DH18\":\"90\",\"DH19\":\"90\",\"DH20\":\"90\",\"DH21\":\"90\",\"DH22\":\"90\",\"DH23\":\"90\""
                    + ",\"WH00\":\"90\",\"WH01\":\"90\",\"WH02\":\"90\",\"WH03\":\"90\",\"WH04\":\"90\",\"WH05\":\"90\""
                    + ",\"WH06\":\"90\",\"WH07\":\"90\",\"WH08\":\"90\",\"WH09\":\"90\",\"WH10\":\"90\",\"WH11\":\"90\""
                    + ",\"WH12\":\"90\",\"WH13\":\"90\",\"WH14\":\"90\",\"WH15\":\"90\",\"WH16\":\"90\",\"WH17\":\"90\""
                    + ",\"WH18\":\"90\",\"WH19\":\"90\",\"WH20\":\"90\",\"WH21\":\"90\",\"WH22\":\"90\",\"WH23\":\"90\""
                    + "}";

            String testMessage = "[2,\"" + uuid + "\",\"DataTransfer\","
                    + "{\"vendorId\":\"DONGAH\","
                    + "\"messageId\":\"rechgrsocschedule.req\","
                    + "\"data\":\"" + dataJson.replace("\"", "\\\"") + "\"}]";

            logger.info("[TEST] rechgrsocschedule2.req 전문 주입: {}", testMessage);
            activity.getSocketReceiveMessage().onGetMessage(null, testMessage);
            logger.info("[TEST] rechgrsocschedule2.req 처리 완료");
        } catch (Exception e) {
            logger.error("[TEST] rechgrsocschedule2.req 테스트 실패: {}", e.getMessage(), e);
        }
    }

    private void onTestRechgrsocscheduleCommon(int connectorId, String socValue, String logName) {
        try {
            String uuid = UUID.randomUUID().toString();

            StringBuilder dataJsonBuilder = new StringBuilder();
            dataJsonBuilder.append("{")
                    .append("\"connectorId\":").append(connectorId);

            for (int hour = 0; hour < 24; hour++) {
                dataJsonBuilder.append(",")
                        .append("\"DH")
                        .append(String.format("%02d", hour))
                        .append("\":\"")
                        .append(socValue)
                        .append("\"");
            }

            for (int hour = 0; hour < 24; hour++) {
                dataJsonBuilder.append(",")
                        .append("\"WH")
                        .append(String.format("%02d", hour))
                        .append("\":\"")
                        .append(socValue)
                        .append("\"");
            }

            dataJsonBuilder.append("}");

            String dataJson = dataJsonBuilder.toString();

            String testMessage = "[2,\"" + uuid + "\",\"DataTransfer\","
                    + "{\"vendorId\":\"DONGAH\","
                    + "\"messageId\":\"rechgrsocschedule.req\","
                    + "\"data\":\"" + dataJson.replace("\"", "\\\"") + "\"}]";

            logger.info("[TEST] {} 전문 주입: {}", logName, testMessage);

            activity.getSocketReceiveMessage().onGetMessage(null, testMessage);

            logger.info("[TEST] {} 처리 완료", logName);
        } catch (Exception e) {
            logger.error("[TEST] {} 테스트 실패: {}", logName, e.getMessage(), e);
        }
    }

    private void onTestWebSocketUrl() {
        try {
            String uuid = UUID.randomUUID().toString();

            String testMessage = "[2,\"" + uuid + "\",\"ChangeConfiguration\","
                    + "{\"key\":\"webSocketURL\","
                    + "\"value\":\"ws://dongahtest.p-e.kr:5000/v2/DAE000202\"}]";

            logger.info("[TEST] ChangeConfiguration webSocketURL 전문 주입: {}", testMessage);

            activity.getSocketReceiveMessage().onGetMessage(null, testMessage);

            logger.info("[TEST] ChangeConfiguration webSocketURL 처리 완료");
        } catch (Exception e) {
            logger.error("[TEST] ChangeConfiguration webSocketURL 테스트 실패", e);
        }
    }

}