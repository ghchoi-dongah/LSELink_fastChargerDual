package com.dongah.fastcharger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.dongah.fastcharger.basefunction.ChargerConfiguration;
import com.dongah.fastcharger.basefunction.ChargingCurrentData;
import com.dongah.fastcharger.basefunction.ClassUiProcess;
import com.dongah.fastcharger.basefunction.ConfigurationKeyRead;
import com.dongah.fastcharger.controlboard.ControlBoard;
import com.dongah.fastcharger.rfcard.RfCardReaderReceive;
import com.dongah.fastcharger.sqlite.SQLiteHelper;
import com.dongah.fastcharger.basefunction.FragmentChange;
import com.dongah.fastcharger.basefunction.FragmentCurrent;
import com.dongah.fastcharger.basefunction.GlobalVariables;
import com.dongah.fastcharger.basefunction.UiSeq;
import com.dongah.fastcharger.utils.DatabaseHttpServer;
import com.dongah.fastcharger.utils.ToastPositionMake;
import com.dongah.fastcharger.websocket.ocpp.core.ChargePointStatus;
import com.dongah.fastcharger.websocket.ocpp.core.Reason;
import com.dongah.fastcharger.websocket.ocpp.utilities.ZonedDateTimeConvert;
import com.dongah.fastcharger.websocket.socket.SocketReceiveMessage;
import com.dongah.fastcharger.websocket.socket.SocketState;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.ChangeModeThread;
import com.dongah.fastcharger.websocket.socket.handler.handlersend.ProcessHandler;
import com.dongah.fastcharger.utils.MonitorHttpServer;
import com.dongah.fastcharger.websocket.tcpsocket.ClientSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {

    public static final Logger logger = LoggerFactory.getLogger(MainActivity.class);

    @SuppressLint("StaticFieldLeak")
    public static Context mContext;

    Handler handler = new Handler();
    Runnable runnable;

    TextView textViewVersion, textViewTime;
    ImageView imgNetwork;

    SQLiteHelper sqLiteHelper;
    SQLiteDatabase sqLiteDatabase;


    UiSeq[] fragmentSeq;
    ClassUiProcess[] classUiProcess;
    ChargingCurrentData[] chargingCurrentData;
    ConfigurationKeyRead configurationKeyRead;
    ChargerConfiguration chargerConfiguration;
    SocketReceiveMessage socketReceiveMessage;
    FragmentChange fragmentChange;
    FragmentCurrent fragmentCurrent;
    ProcessHandler processHandler;

    ControlBoard controlBoard;
    RfCardReaderReceive rfCardReaderReceive;
    ToastPositionMake toastPositionMake;
    ClientSocket clientSocket;
    MonitorHttpServer monitorHttpServer;
    DatabaseHttpServer databaseHttpServer;


    public UiSeq getFragmentSeq(int ch)  {
        return fragmentSeq[ch];
    }

    public void setFragmentSeq(int ch, UiSeq fragmentSeq) {
        this.fragmentSeq[ch] = fragmentSeq;
    }

    public ClassUiProcess[] getClassUiProcess() {
        return classUiProcess;
    }

    public ClassUiProcess getClassUiProcess(int ch) {
        return classUiProcess[ch];
    }

    public ChargingCurrentData getChargingCurrentData(int ch) {
        return chargingCurrentData[ch];
    }

    public ConfigurationKeyRead getConfigurationKeyRead() {
        return configurationKeyRead;
    }

    public ChargerConfiguration getChargerConfiguration() {
        return chargerConfiguration;
    }

    public SocketReceiveMessage getSocketReceiveMessage() {
        return socketReceiveMessage;
    }

    public ControlBoard getControlBoard() {
        return controlBoard;
    }

    public RfCardReaderReceive getRfCardReaderReceive() {
        return rfCardReaderReceive;
    }

    public ToastPositionMake getToastPositionMake() {
        return toastPositionMake;
    }

    public FragmentChange getFragmentChange() {
        return fragmentChange;
    }

    public ProcessHandler getProcessHandler() {
        return processHandler;
    }


    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        hideNavigationBar();

        // 앱 켜질 때 타이머 시작
//        resetInactivityTimer();

        mContext = this;

        /* 슬립 모드 방지*/
        super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        /* 세로 고정 */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        // SQLite DB
        sqLiteHelper = new SQLiteHelper(this);
        sqLiteDatabase = sqLiteHelper.getWritableDatabase();
//        sqLiteHelper.dropAllTables(sqLiteDatabase);   // delete all tables
//        sqLiteHelper.onCreate(sqLiteDatabase);          // create all tables

        imgNetwork = findViewById(R.id.imageViewNetwork);
        textViewVersion = findViewById(R.id.textViewVersionValue);
        textViewTime = findViewById(R.id.textViewTime);

        // fragment current
        fragmentCurrent = new FragmentCurrent();
        toastPositionMake = new ToastPositionMake(this);

        // 0. ConfigurationKey read
        configurationKeyRead = new ConfigurationKeyRead();
        configurationKeyRead.onRead();

        // charger operation
        onChargerOperate();

        // 1. charger configuration, ConfigurationKey read
        chargerConfiguration = new ChargerConfiguration();
        chargerConfiguration.onLoadConfiguration();
        chargerConfiguration.setSigned(true);
        textViewVersion.setText("VER-DEVS " + GlobalVariables.VERSION + " | ");

        // 2. fragment change management
        fragmentChange = new FragmentChange();
        fragmentSeq = new UiSeq[GlobalVariables.maxChannel];
        chargingCurrentData = new ChargingCurrentData[GlobalVariables.maxChannel];
        for (int i = 0; i < GlobalVariables.maxChannel; i++) {
            fragmentChange.onFragmentChange(i, UiSeq.INIT, "INIT", "");
            fragmentChange.onFragmentHeaderChange(i, "Header");
            chargingCurrentData[i] = new ChargingCurrentData();
            chargingCurrentData[i].onCurrentDataClear();
        }

        // 3. control board
        controlBoard = new ControlBoard(GlobalVariables.maxChannel, chargerConfiguration.getControlCom());

        // 4. rf card reader : MID = terminal ID
        rfCardReaderReceive = new RfCardReaderReceive(chargerConfiguration.getRfCom());

        // Web Monitor Server
        if (chargerConfiguration.isControlMonitor()) {
            monitorHttpServer = new MonitorHttpServer(8080);
            monitorHttpServer.start();
        }

        // Database Inspector Server
        databaseHttpServer = new DatabaseHttpServer();
        databaseHttpServer.start();

        /**
         *  개발 ocpp 서버 url :
         *  ws://dev-connect.lselink.com/ocpp/{충전소ID}{충전기ID}
         *  ws://dev-connect.lselink.com/ocpp/00000026
         *  websocketUrl : ws://dev-connect.lselink.com/ocpp/
         *  충전소ID : 000000
         *  충전기ID : 26
         */
        chargerConfiguration.setSigned(false);

        // 5. handler
        processHandler = new ProcessHandler();

        // 6. socket
//        String baseUrl = "ws://dongahtest.p-e.kr:5000/v2/DAE000202";
        String baseUrl =  chargerConfiguration.getServerConnectingString() + chargerConfiguration.getChargeBoxSerialNumber() + chargerConfiguration.getChargerId();
        socketReceiveMessage = new SocketReceiveMessage(baseUrl);

        SocketState state = socketReceiveMessage.getSocket().getState();

        /** opMode
         * 0: test mode
         * 1: server mode
         **/
        if (Objects.equals(chargerConfiguration.getOpMode(), 1)) {
            onChangeMode(sqLiteHelper); // change mode
        }

        if (state != SocketState.OPEN || Objects.equals(chargerConfiguration.getOpMode(), 0)) {
            // 전류, SoC 제한 설정
            for (int i = 0; i <GlobalVariables.maxChannel; i++) {
                ((MainActivity) MainActivity.mContext).getControlBoard().getTxData(i).setOutPowerLimit((short) chargerConfiguration.getDr());
                ((MainActivity) MainActivity.mContext).getChargingCurrentData(i).setLimitSoc(chargerConfiguration.getTargetSoc());
            }
        }

        // 7. classUiProcess
        classUiProcess = new ClassUiProcess[GlobalVariables.maxChannel];
        for (int i = 0; i < GlobalVariables.maxChannel; i++) {
            classUiProcess[i] = new ClassUiProcess(i);
            classUiProcess[i].setUiSeq(UiSeq.INIT);

        }

        // 8. PLC modem
        clientSocket = new ClientSocket("192.168.39.1", 9999, new ClientSocket.TcpClientListener() {
            @Override
            public void onConnected() {
                logger.debug("connected");

                clientSocket.start();
                clientSocket.sendCommandExpectPrefix("AT+CNUM", "+CNUM:", 10000)
                        .thenApply(line -> {
                            // line 예: +CNUM: "LGU","+821222492396",145
                            String[] parts = line.split(",");
                            String raw = parts.length >= 2 ? parts[1].replace("\"","") : null;
                            GlobalVariables.setIMSI(raw == null ? "" : parseToLocal(raw));
                            return parseToLocal(raw); // 01222492396
                        })
                        .thenCompose(localNumber -> {
                            Log.d("TCP","Parsed local number: " + localNumber);
                            // 이어서 DSCREEN 명령
                            return clientSocket.sendCommandExpectPrefix("AT$$DSCREEN?", "DSCREEN:", 5000);
                        })
                        .thenAccept(dscreenResp -> {
                            GlobalVariables.setRSRP(parseToRSRP(dscreenResp));
                            Log.d("TCP","DSCREEN response: " + dscreenResp);
                            clientSocket.postDisconnected();
                            clientSocket.closeSocket();
                        })
                        .exceptionally(ex -> {
                            Log.e("TCP","Command chain error", ex);
                            return null;
                        });
            }

            @Override
            public void onDisconnected() {

            }

            @Override
            public void onError(Exception e) {

            }

            @Override
            public void onMessageReceived(String message) {
                Log.d("TCP", "General recv: "+ message);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        runnable = new Runnable() {
            @Override
            public void run() {
                updateTime();
                // 1초마다 실행
                handler.postDelayed(this, 1000);
                try {
                    if (socketReceiveMessage.getSocket().getState() != null) {
                        imgNetwork.setBackgroundResource(socketReceiveMessage.getSocket().getState() == SocketState.OPEN ?
                                R.drawable.network : R.drawable.nonetwork);
                    }
                } catch (Exception e) {
                    logger.error("MainActivity onStart error : {}", e.getMessage());
                }
            }
        };
        runnable.run();
    }

    private void updateTime() {
        try {
            if (textViewTime != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                String currentTime = sdf.format(new Date());
                textViewTime.setText(currentTime);
            }
        } catch (Exception e) {
            logger.error("MainActivity updateTime error : {}", e.getMessage());
        }
    }

    private void hideNavigationBar() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }


    // screen saver
    private static final long INACTIVITY_TIMEOUT = 1 * 60 * 1000L;  // 1분 (ms 단위)
    private final Handler inactivityHandler = new Handler(Looper.getMainLooper());
//    private final Runnable inactivityRunnable = new Runnable() {
//        @Override
//        public void run() {
//            try {
//                boolean check = fragmentChange.onFragmentScreenSaverChange();
//                if (!check) {
//                    resetInactivityTimer();
//                }
//            } catch (Exception e) {
//                logger.error("ScreenSaver inactivityRunnable error : {}", e.getMessage());
//            }
//        }
//    };
//
//
//    // 타이머 리셋 메서드 (외부에서 호출 가능)
//    public void resetInactivityTimer() {
//        inactivityHandler.removeCallbacks(inactivityRunnable);
//        inactivityHandler.postDelayed(inactivityRunnable, INACTIVITY_TIMEOUT);
//    }
//
//    @Override
//    public void onUserInteraction() {
//        super.onUserInteraction();
//        resetInactivityTimer();  // 입력 있을 때마다 타이머 리셋
//    }

    /** ui version update */
    public void onRebooting() {
        try {
            boolean result = false;
            ChargingCurrentData chargingCurrentData;
            for (int i = 0; i < GlobalVariables.maxChannel; i++) {
                chargingCurrentData = ((MainActivity) MainActivity.mContext).getChargingCurrentData(i);
                result = chargingCurrentData.isReBoot() && (getClassUiProcess(i).getUiSeq() == UiSeq.INIT);
            }

            if (result) {
                for (int i = 0; i < GlobalVariables.maxChannel; i++) {
                    getClassUiProcess(i).setUiSeq(UiSeq.REBOOTING);
                    ((MainActivity) MainActivity.mContext).getChargingCurrentData(i).setStopReason(Reason.Reboot);
                }
            }
        } catch (Exception e) {
            logger.error("MainActivity version reboot : {}", e.getMessage());
        }
    }

    @SuppressLint("ConstantConditions")
    public void onRebooting(String type) {
        try {
            ((MainActivity) MainActivity.mContext).getSocketReceiveMessage().getSocket().disconnect();
            if (Objects.equals(type, "Soft")) {
                ActivityCompat.finishAffinity(((MainActivity) MainActivity.mContext));
                System.exit(0);
            } else {
                try {
                    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    powerManager.reboot("reboot");
                } catch (SecurityException se) {
                    logger.error("System reboot permission denied. Trying su reboot... : {}", se.getMessage());
                    try {
                        // Root 권한인 경우 reboot 명령 실행 시도
                        Runtime.getRuntime().exec("su -c reboot");
                    } catch (Exception re) {
                        logger.error("su reboot failed: {}. Falling back to Soft reboot.", re.getMessage());
                        // reboot 실패 시 앱 종료 후 워치독/런처에 의한 재시작 유도
                        ActivityCompat.finishAffinity(((MainActivity) MainActivity.mContext));
                        System.exit(0);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("onRebooting error : {}", e.getMessage());
        }
    }

    private void onChargerOperate() {
        try {
            File file = new File(GlobalVariables.getRootPath() + File.separator + "ChargerOperate");
            if (file.exists()) {
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line;
                int count = 0;
                while ((line = bufferedReader.readLine()) != null) {
                    GlobalVariables.ChargerOperation[count] = Objects.equals(line, "true");
                    count++;
                }
            } else {
                Arrays.fill(GlobalVariables.ChargerOperation, true);
            }
        } catch (Exception e) {
            logger.error("onChargerOperate error : {}", e.getMessage());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void onChangeMode(SQLiteHelper helper) {
        try {
            String tableName = "CP_CHANGE_MODE";
            if (!helper.isTableExists(helper, tableName)) {
                helper.onCreateTable(sqLiteDatabase, tableName);
                for (int i = 1; i <= GlobalVariables.maxChannel; i++) {
                    ChangeModeThread.insertChgMode(helper, i);
                }
            }

            ZonedDateTime now = new ZonedDateTimeConvert().doGetCurrentTime();
            if (now == null) return;

            int hour = now.getHour();
            @SuppressLint("DefaultLocale") String hourKey = String.format("HH%02d", hour);


            for (int i = 1; i <= GlobalVariables.maxChannel; i++) {
                Cursor cursor = helper.select(tableName,"CONNECTOR_ID=?", new String[]{String.valueOf(i)});
                ChargingCurrentData currentData = ((MainActivity) MainActivity.mContext).getChargingCurrentData(i-1);

                if (cursor == null || !cursor.moveToFirst()) {
                    ChangeModeThread.insertChgMode(helper, i);
                    continue;
                }

                String value = cursor.getString(cursor.getColumnIndexOrThrow(hourKey));
                System.out.println("processChgMode " + hourKey + " : " + value);

                switch (value) {
                    case "DM":
                        currentData.setChangeMode(value);
                        currentData.setConnectUse(true);
                        break;
                    case "NM":
                        currentData.setChangeMode(value);
                        ChargePointStatus targetStatus;
                        int priority = chargerConfiguration.getConnectorPriority();
                        if (priority == 1) {
                            targetStatus = i != 1 ? ChargePointStatus.Unavailable : ChargePointStatus.Available;
                        } else {
                            targetStatus = i != 2 ? ChargePointStatus.Unavailable : ChargePointStatus.Available;
                        }
                        currentData.setConnectUse(targetStatus.equals(ChargePointStatus.Available));
                        break;
                    default:
                        currentData.setChangeMode(value);
                        currentData.setConnectUse(false);
                        break;
                }
            }
        } catch (Exception e) {
            logger.error("onChangeMode error : {}", e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
//        inactivityHandler.removeCallbacks(inactivityRunnable);
        handler.removeCallbacks(runnable);
        for (int i = 0; i < GlobalVariables.maxChannel; i++) {
            classUiProcess[i].stopEventLoop();
        }
        if (monitorHttpServer != null) {
            monitorHttpServer.stopServer();
        }
        super.onDestroy();
    }

    private String parseToLocal(String number) {
        if (number.startsWith("+82")) {
            return "0" + number.substring(3);
        }
        return number;
    }

    private String parseToRSRP(String resp) {
        Pattern p = Pattern.compile("RSRP:([-]?\\d+)");
        Matcher m = p.matcher(resp);
        if (m.find()) {
            int rsrp = Integer.parseInt(m.group(1));  // -71
            return String.valueOf(rsrp);
        } else {
            System.out.println("RSRP not found");
        }
        return "";
    }

    // 키보드 내리기
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View view = getCurrentFocus();

        if (view != null) {
            int[] scrcoords = new int[2];
            view.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + view.getLeft() - scrcoords[0];
            float y = ev.getRawY() + view.getTop() - scrcoords[1];

            if (ev.getAction() == MotionEvent.ACTION_UP &&
                    (x < view.getLeft() || x >= view.getRight() ||
                            y < view.getTop() || y > view.getBottom())) {

                // 키보드 내리기
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

                // EditText 포커스 제거
                view.clearFocus();
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}