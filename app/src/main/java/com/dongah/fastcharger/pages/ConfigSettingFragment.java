package com.dongah.fastcharger.pages;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.dongah.fastcharger.MainActivity;
import com.dongah.fastcharger.R;
import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.GlobalVariables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConfigSettingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConfigSettingFragment extends Fragment implements View.OnClickListener {

    private static final Logger logger = LoggerFactory.getLogger(ConfigSettingFragment.class);

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    ChargerConfiguration chargerConfiguration;
    InputMethodManager imm;
    Spinner spinnerChargePointType, spinnerChargePointModel, spinnerAuthMode, spinnerOpMode, spinnerStartMode;
    int spPosition = 0, spChargerPointModelCode = 0, spAuthMode = 0, spOpMode = 0, spStartMode = 0;
    EditText editChargeBoxSerialNumber, editChargerId;
    EditText editServerUrl, editServerPort, editControlPort, editRfPort, editCreditCardPort;
    EditText editTestPrice, editConnectorPriority;
    EditText editChargePointSerialNumber, editChargePointVendor;
    EditText editMid;
    EditText editFirmwareVersion;
    EditText editIccid, editImsi;
    EditText editMeterSerialNumber, editMeterType;
    EditText editSoc, editDR;
    Button btnExit, btnSave, btnRebooting, btnKeyboardControl;
    CheckBox checkboxControlMonitor, checkboxInitInfo;


    public ConfigSettingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ConfigSettingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ConfigSettingFragment newInstance(String param1, String param2) {
        ConfigSettingFragment fragment = new ConfigSettingFragment();
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
        View view = inflater.inflate(R.layout.fragment_config_setting, container, false);
        try {
            chargerConfiguration = ((MainActivity) MainActivity.mContext).getChargerConfiguration();
            imm = (InputMethodManager) (MainActivity.mContext).getSystemService(Context.INPUT_METHOD_SERVICE); // keyboard hidden
            btnKeyboardControl = view.findViewById(R.id.btnKeyboardControl);
            btnKeyboardControl.setOnClickListener(this);
            btnExit = view.findViewById(R.id.btnExit);
            btnExit.setOnClickListener(this);
            btnSave = view.findViewById(R.id.btnSave);
            btnSave.setOnClickListener(this);
            btnRebooting = view.findViewById(R.id.btnRebooting);
            btnRebooting.setOnClickListener(this);

            // chargerPointType
            spinnerChargePointType = view.findViewById(R.id.spinnerChargePointType);
            ArrayAdapter<CharSequence> chargerTypeAdapter = ArrayAdapter.createFromResource(MainActivity.mContext, R.array.chargerType, R.layout.spinner_item);
            chargerTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerChargePointType.setAdapter(chargerTypeAdapter);
            spinnerChargePointType.setSelection(chargerConfiguration.getChargerPointType() - 1);
            spinnerChargePointType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    spPosition = position + 1;
                    chargerConfiguration.setChargerPointType(position + 1);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            // chargerPointModel
            spinnerChargePointModel = view.findViewById(R.id.spinnerChargePointModel);
            ArrayAdapter<CharSequence> mcuTypeAdapter = ArrayAdapter.createFromResource(MainActivity.mContext, R.array.chargerModel, R.layout.spinner_item);
            mcuTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerChargePointModel.setAdapter(mcuTypeAdapter);
            spinnerChargePointModel.setSelection(chargerConfiguration.getChargerPointModelCode());
            spinnerChargePointModel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    chargerConfiguration.setChargerPointModelCode(position);
                    Resources resources = getResources();
                    String[] chargerModel = resources.getStringArray(R.array.chargerModel);
                    chargerConfiguration.setChargerPointModel(chargerModel[position]);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });

            // authMode
            spinnerAuthMode = view.findViewById(R.id.spinnerAuthMode);
            ArrayAdapter<CharSequence> authAdapter = ArrayAdapter.createFromResource(MainActivity.mContext, R.array.authMode, R.layout.spinner_item);
            authAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerAuthMode.setAdapter(authAdapter);
            spinnerAuthMode.setSelection(chargerConfiguration.getAuthMode());
            spinnerAuthMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    spAuthMode = position;
                    chargerConfiguration.setAuthMode(spAuthMode);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            // opMode
            spinnerOpMode = view.findViewById(R.id.spinnerOpMode);
            ArrayAdapter<CharSequence> opAdapter = ArrayAdapter.createFromResource(MainActivity.mContext, R.array.opMode, R.layout.spinner_item);
            opAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerOpMode.setAdapter(opAdapter);
            spinnerOpMode.setSelection(chargerConfiguration.getOpMode());
            spinnerOpMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    spOpMode = position;
                    chargerConfiguration.setOpMode(spOpMode);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            // startMode
            spinnerStartMode = view.findViewById(R.id.spinnerStartMode);
            ArrayAdapter<CharSequence> startAdapter = ArrayAdapter.createFromResource(MainActivity.mContext, R.array.startMode, R.layout.spinner_item);
            startAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerStartMode.setAdapter(startAdapter);
            spinnerStartMode.setSelection(chargerConfiguration.getStartMode());
            spinnerStartMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    spStartMode = position;
                    chargerConfiguration.setStartMode(spStartMode);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            InitializationComponents(view);

        } catch (Exception e) {
            logger.error("ConfigSettingFragment onCreateView error : {}", e.getMessage());
        }
        return view;
    }

    @Override
    public void onClick(View v) {
        if (Objects.equals(v.getId(), R.id.btnExit)) {
            try {
                FragmentTransaction transaction = ((MainActivity) MainActivity.mContext).getSupportFragmentManager().beginTransaction();
                EnvironmentFragment environmentFragment = new EnvironmentFragment();
                transaction.replace(R.id.frameFull, environmentFragment);
                transaction.commit();
            } catch (Exception e) {
                logger.error("ConfigSettingFragment fragment change fail : {}", e.getMessage());
            }
        } else if (Objects.equals(v.getId(), R.id.btnSave)) {
            // 필수값 확인
            if (TextUtils.isEmpty(editChargeBoxSerialNumber.getText().toString())) {
                editChargeBoxSerialNumber.setFocusableInTouchMode(true);
                editChargeBoxSerialNumber.requestFocus();
                Toast.makeText(requireContext(), getString(R.string.configRequired, getString(R.string.chargeBoxSerialNumber)), Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(editChargerId.getText().toString())) {
                editChargerId.setFocusableInTouchMode(true);
                editChargerId.requestFocus();
                Toast.makeText(requireContext(), getString(R.string.configRequired, getString(R.string.chargerId)), Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(editServerUrl.getText().toString())) {
                editServerUrl.setFocusableInTouchMode(true);
                editServerUrl.requestFocus();
                Toast.makeText(requireContext(), getString(R.string.configRequired, getString(R.string.serverUrl)), Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(editServerPort.getText().toString())) {
                editServerPort.setFocusableInTouchMode(true);
                editServerPort.requestFocus();
                Toast.makeText(requireContext(), getString(R.string.configRequired, getString(R.string.serverPort)), Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(editControlPort.getText().toString())) {
                editControlPort.setFocusableInTouchMode(true);
                editControlPort.requestFocus();
                Toast.makeText(requireContext(), getString(R.string.configRequired, getString(R.string.controlPort)), Toast.LENGTH_SHORT).show();
                return;

            } else if (TextUtils.isEmpty(editRfPort.getText().toString())) {
                editRfPort.setFocusableInTouchMode(true);
                editRfPort.requestFocus();
                Toast.makeText(requireContext(), getString(R.string.configRequired, getString(R.string.rfPort)), Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(editCreditCardPort.getText().toString())) {
                editCreditCardPort.setFocusableInTouchMode(true);
                editCreditCardPort.requestFocus();
                Toast.makeText(requireContext(), getString(R.string.configRequired, getString(R.string.creditCardPort)), Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(editTestPrice.getText().toString())) {
                editTestPrice.setFocusableInTouchMode(true);
                editTestPrice.requestFocus();
                Toast.makeText(requireContext(), getString(R.string.configRequired, getString(R.string.testPrice)), Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(editConnectorPriority.getText().toString())) {
                editConnectorPriority.setFocusableInTouchMode(true);
                editConnectorPriority.requestFocus();
                Toast.makeText(requireContext(), getString(R.string.configRequired, getString(R.string.connectorPriority)), Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(editChargePointSerialNumber.getText().toString())) {
                editChargePointSerialNumber.setFocusableInTouchMode(true);
                editChargePointSerialNumber.requestFocus();
                Toast.makeText(requireContext(), getString(R.string.configRequired, getString(R.string.chargePointSerialNumber)), Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(editChargePointVendor.getText().toString())) {
                editChargePointVendor.setFocusableInTouchMode(true);
                editChargePointVendor.requestFocus();
                Toast.makeText(requireContext(), getString(R.string.configRequired, getString(R.string.chargePointVendor)), Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(editMid.getText().toString())) {
                editMid.setFocusableInTouchMode(true);
                editMid.requestFocus();
                Toast.makeText(requireContext(), getString(R.string.configRequired, getString(R.string.mid)), Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(editFirmwareVersion.getText().toString())) {
                editFirmwareVersion.setFocusableInTouchMode(true);
                editFirmwareVersion.requestFocus();
                Toast.makeText(requireContext(), getString(R.string.configRequired, getString(R.string.firmwareVersion)), Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(editSoc.getText().toString())) {
                editSoc.setFocusableInTouchMode(true);
                editSoc.requestFocus();
                Toast.makeText(requireContext(), getString(R.string.configRequired, getString(R.string.soc)), Toast.LENGTH_SHORT).show();
                return;
            } else if (TextUtils.isEmpty(editDR.getText().toString())) {
                editDR.setFocusableInTouchMode(true);
                editDR.requestFocus();
                Toast.makeText(requireContext(), getString(R.string.configRequired, getString(R.string.dr)), Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.configSaveTitle)
                    .setMessage(R.string.configSaveYesNo)
                    .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            onSaveConfiguration();

                            try {
                                if (Objects.equals(chargerConfiguration.getOpMode(), 0)) {
                                    for (int j = 0; j < GlobalVariables.maxChannel; j++) {
                                        // 전류 제한 설정
                                        ((MainActivity) MainActivity.mContext).getControlBoard().getTxData(j).setOutPowerLimit((short) Integer.parseInt(editDR.getText().toString()));

                                        // SoC 제한 설정
                                        String socText = editSoc.getText().toString().trim();
                                        if (!socText.isEmpty()) {
                                            ((MainActivity) MainActivity.mContext).getChargingCurrentData(j).setLimitSoc(Integer.parseInt(socText));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.error("AlertDialog save error : {}", e.getMessage(), e);
                            }

                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getActivity(), R.string.configSaveSuccess, Toast.LENGTH_SHORT).show();
                                }
                            }, 50);
                        }
                    })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            InitializationComponents(getView());
                            Toast.makeText(requireContext(), R.string.configSaveCancel, Toast.LENGTH_SHORT).show();
                        }
                    }).show();
        } else if (Objects.equals(v.getId(), R.id.btnRebooting)) {
            ((MainActivity) MainActivity.mContext).onRebooting("Hard");
        } else if (Objects.equals(v.getId(), R.id.btnKeyboardControl)) {
            try {
                View view = ((MainActivity) MainActivity.mContext).getCurrentFocus();
                if (view instanceof EditText) {
                    EditText editText = (EditText) ((MainActivity) MainActivity.mContext).getCurrentFocus();
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                }
            } catch (Exception e) {
                logger.error("config - onClick {}", e.getMessage());
            }
        }
    }

    private void InitializationComponents(View v) {
        try {
            spPosition = chargerConfiguration.getChargerPointType();
            spChargerPointModelCode = chargerConfiguration.getChargerPointModelCode();
            spAuthMode = chargerConfiguration.getAuthMode();
            spOpMode = chargerConfiguration.getOpMode();
            spStartMode = chargerConfiguration.getStartMode();

            editChargeBoxSerialNumber = v.findViewById(R.id.editChargeBoxSerialNumber);
            editChargeBoxSerialNumber.setText(chargerConfiguration.getChargeBoxSerialNumber());
            editChargerId = v.findViewById(R.id.editChargerId);
            editChargerId.setText(chargerConfiguration.getChargerId());

            editServerUrl = v.findViewById(R.id.editServerUrl);
            editServerUrl.setText(chargerConfiguration.getServerConnectingString());
            editServerPort = v.findViewById(R.id.editServerPort);
            editServerPort.setText(String.valueOf(chargerConfiguration.getServerPort()));

            editControlPort = v.findViewById(R.id.editControlPort);
            editControlPort.setText(chargerConfiguration.getControlCom());
            editRfPort = v.findViewById(R.id.editRfPort);
            editRfPort.setText(chargerConfiguration.getRfCom());
            editCreditCardPort = v.findViewById(R.id.editCreditCardPort);
            editCreditCardPort.setText(chargerConfiguration.getCreditCom());

            editTestPrice = v.findViewById(R.id.editTestPrice);
            editTestPrice.setText(chargerConfiguration.getTestPrice());
            editConnectorPriority = v.findViewById(R.id.editConnectorPriority);
            editConnectorPriority.setText(String.valueOf(chargerConfiguration.getConnectorPriority()));

            editChargePointSerialNumber = v.findViewById(R.id.editChargePointSerialNumber);
            editChargePointSerialNumber.setText(chargerConfiguration.getChargePointSerialNumber());
            editChargePointVendor = v.findViewById(R.id.editChargePointVendor);
            editChargePointVendor.setText(chargerConfiguration.getChargePointVendor());
            editMid = v.findViewById(R.id.editMid);
            editMid.setText(chargerConfiguration.getMID());
            editFirmwareVersion = v.findViewById(R.id.editFirmwareVersion);
            editFirmwareVersion.setText(chargerConfiguration.getFirmwareVersion());
            editIccid = v.findViewById(R.id.editIccid);
            editIccid.setText(chargerConfiguration.getIccid());
            editImsi = v.findViewById(R.id.editImsi);
            editImsi.setText(chargerConfiguration.getImsi());
            editMeterSerialNumber = v.findViewById(R.id.editMeterSerialNumber);
            editMeterSerialNumber.setText(chargerConfiguration.getMeterSerialNumber());
            editMeterType = v.findViewById(R.id.editMeterType);
            editMeterType.setText(chargerConfiguration.getMeterType());
            editSoc = v.findViewById(R.id.editSoc);
            editSoc.setText(String.valueOf(chargerConfiguration.getTargetSoc()));
            editDR = v.findViewById(R.id.editDR);
            editDR.setText(String.valueOf(chargerConfiguration.getDr()));
            checkboxControlMonitor = v.findViewById(R.id.checkboxControlMonitor);
            checkboxControlMonitor.setChecked(chargerConfiguration.isControlMonitor());
            checkboxInitInfo = v.findViewById(R.id.checkboxInitInfo);
            checkboxInitInfo.setChecked(chargerConfiguration.isInitInfo());
        } catch (Exception e) {
            logger.error("ConfigSettingFragment InitializationComponents error : {}",  e.getMessage());
        }
    }

    private void onSaveConfiguration() {
        try {
            onConfigurationUpdate();
            chargerConfiguration.onSaveConfiguration();
            chargerConfiguration.onLoadConfiguration();
        } catch (Exception e) {
            logger.error("ConfigSettingFragment onSaveConfiguration error : {}",  e.getMessage());
        }
    }

    private void onConfigurationUpdate() {
        try {
            chargerConfiguration.setChargerPointType(spPosition);
            chargerConfiguration.setChargerPointModelCode(spChargerPointModelCode);
            chargerConfiguration.setAuthMode(spAuthMode);
            chargerConfiguration.setOpMode(spOpMode);
            chargerConfiguration.setStartMode(spStartMode);

            chargerConfiguration.setChargeBoxSerialNumber(editChargeBoxSerialNumber.getText().toString());
            chargerConfiguration.setChargerId(editChargerId.getText().toString());
            chargerConfiguration.setServerConnectingString(editServerUrl.getText().toString());
            chargerConfiguration.setServerPort(Integer.parseInt(editServerPort.getText().toString()));
            chargerConfiguration.setControlCom(editControlPort.getText().toString());
            chargerConfiguration.setRfCom(editRfPort.getText().toString());
            chargerConfiguration.setCreditCom(editCreditCardPort.getText().toString());

            chargerConfiguration.setTestPrice(editTestPrice.getText().toString());
            chargerConfiguration.setConnectorPriority(Integer.parseInt(editConnectorPriority.getText().toString()));

            chargerConfiguration.setChargePointSerialNumber(editChargePointSerialNumber.getText().toString());
            chargerConfiguration.setChargePointVendor(editChargePointVendor.getText().toString());
            chargerConfiguration.setMID(editMid.getText().toString());
            chargerConfiguration.setFirmwareVersion(editFirmwareVersion.getText().toString());
            chargerConfiguration.setIccid(editIccid.getText().toString());
            chargerConfiguration.setImsi(editImsi.getText().toString());
            chargerConfiguration.setMeterSerialNumber(editMeterSerialNumber.getText().toString());
            chargerConfiguration.setMeterType(editMeterType.getText().toString());
            chargerConfiguration.setTargetSoc(Integer.parseInt(editSoc.getText().toString()));
            chargerConfiguration.setDr(Integer.parseInt(editDR.getText().toString()));

            chargerConfiguration.setControlMonitor(checkboxControlMonitor.isChecked());
            chargerConfiguration.setInitInfo(checkboxInitInfo.isChecked());
        } catch (Exception e) {
            logger.error("ConfigSettingFragment onConfigurationUpdate error : {}",  e.getMessage());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}