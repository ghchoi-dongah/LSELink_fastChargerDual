# CLAUDE.md

이 파일은 Claude Code(claude.ai/code)가 이 저장소의 코드를 작업할 때 참고하는 안내 문서입니다.

## 프로젝트 개요

**FastCharger Dual**은 듀얼 채널 전기차 충전기용 Android 애플리케이션입니다. OCPP 1.6 WebSocket 통신, 결제 단말기 연동, 시리얼 하드웨어 제어, RF 카드 리더 기능을 구현합니다.

- 패키지: `com.dongah.fastcharger`
- 최소 SDK: 24 (Android 7.0), 타겟/컴파일 SDK: 36
- 언어: Java (주), Kotlin (Compose UI)
- NDK: `app/src/main/jni/Android.mk`를 통한 시리얼 포트 네이티브 라이브러리

## 빌드 명령어

Android Studio 또는 프로젝트 루트의 Gradle wrapper를 통해 빌드합니다:

```bash
# 디버그 빌드
./gradlew assembleDebug

# 릴리즈 빌드
./gradlew assembleRelease

# 단위 테스트 실행
./gradlew test

# 계측 테스트 실행
./gradlew connectedAndroidTest

# 클린
./gradlew clean
```

릴리즈 서명 설정은 로컬 키스토어 `D:\AndroidDongah\PlatformKeyClear\keystore\platform.jks`를 참조합니다 (플랫폼 키, 별칭 `platform`, 비밀번호 `android`). `senke` 및 `hola` 하드웨어 변형에 대한 주석 처리된 설정은 `app/build.gradle`에 있습니다.

## 아키텍처

### 진입점

`MainActivity.java`는 단일 Activity입니다. 시작 시 모든 서브시스템을 초기화합니다:
- WebSocket OCPP 연결
- 제어 보드 시리얼 통신
- RF 카드 리더
- 결제 단말기 (TECH3800)

Fragment 전환은 `basefunction/FragmentChange.java`가 담당하며, UI 상태 변화에 따라 표시할 Fragment가 결정됩니다.

### 주요 패키지

**`basefunction/`** — 핵심 비즈니스 로직
- `GlobalVariables.java` — 앱 전역 상태; `maxChannel = 2`, `maxPlugCount = 3`
- `ChargerConfiguration.java` — 영구 설정: 서버 URL, 인증 모드, 동작 모드 (`sqlite/SQLiteHelper.java`를 통해 SQLite 읽기/쓰기)
- `ChargingCurrentData.java` — 커넥터별 실시간 충전 상태
- `ClassUiProcess.java` — UI 상태 머신; Fragment 전환 및 충전 흐름 제어
- `NotifyFaultCheck.java` — 오류 감지 로직

**`websocket/socket/`** — WebSocket 전송 계층
- `Socket.java` — OkHttp3 WebSocket 클라이언트; BKS 키스토어로 TLS 처리 (`charging_station_keystore.bks` / `charging_station_truststore.bks`)
- `SocketReceiveMessage.java` — OCPP 메시지 디스패처; 액션 이름을 핸들러 인스턴스에 매핑
- `SocketState.java` — 연결 수명주기 열거형: `NONE → OPEN → RECONNECTING → CLOSED`

**`websocket/socket/handler/`** — OCPP 핸들러 분리:
- `handlersend/` — 주기적/트리거 OCPP 요청을 전송하는 백그라운드 스레드 (HeartbeatThread, BootNotificationThread, StatusNotificationThread 등)
- `handlerreceive/` — 수신 OCPP 명령에 대해 `SocketReceiveMessage`가 호출하는 핸들러 (AuthorizeHandler, ResetHandler, ChangeConfigurationHandler 등)

**`websocket/ocpp/`** — OCPP 1.6 기능 그룹:
- `core/` — 코어 프로파일: Authorize, Start/StopTransaction, MeterValues, Reset, RemoteStart/Stop
- `firmware/` — 펌웨어 업데이트 흐름
- `security/` — 인증서 작업
- `smartcharging/` — 충전 프로파일
- `localauthlist/` — 로컬 인증 목록
- `datatransfer/lselink/` — LSE-Link 벤더 DataTransfer 확장 (결제, 배터리 정보, 차량 정보, 단가 등)
- `datatransfer/dongah/` — 동아 전용 DataTransfer 확장
- `datatransfer/vas/` — VAS (부가가치 서비스) 확장

**`TECH3800/`** — 시리얼 결제 단말기 프로토콜
- `TLS3800.java` — 시리얼 패킷 프로토콜 (RF 카드 읽기, 결제, 취소, IC 체크)
- `packet/` — 패킷 구조: `PacketHeader`, `PacketPay`, `PacketPayG`, `PacketPayCancel`

**`controlboard/`** — 시리얼 하드웨어 제어 보드 (CRC16)
- `ControlBoard.java` — 명령 전송 및 전압/전류/온도/상태 수신
- `RxData.java` / `TxData.java` — 보드 데이터 프레임

**`pages/`** — UI Fragment (Jetpack Compose + View 혼합)
- 충전 흐름: `InitFragment` → `MemberCardFragment` → `ChargingFragment` → `ChargingFinishFragment`
- 상태: `FaultFragment`, `ScreenSaverFragment`, `ConnectionFailedFragment`
- 관리자/디버그: `ConfigSettingFragment`, `WebSocketDebugFragment`, `ProductTestFragment`, `EnvironmentFragment`

**`rfcard/`** — 콜백 리스너 패턴의 RF/NFC 카드 리더

**`sqlite/`** — SQLite 영속성: `CpSettings` (충전기 설정), `CpNonTransmit` (오프라인 거래 버퍼)

**`utils/`** — `CRC16.java`, `FileManagement.java`, `LogDataSave.java`, `MonitorHttpServer.java`

### AIDL 인터페이스

`app/src/main/aidl/`에 위치:
- `service/vact/smartro/com/vcat/SmartroVCatInterface.aidl` — V-CAT 결제 서비스 (executeService, postExtraData, cancelService)
- `service/vact/smartro/com/vcat/SmartroVCatCallback.aidl` — V-CAT 콜백 (onServiceEvent, onServiceResult)
- `com/dongah/fastcharger/VCatConstructor.java` — AIDL 헬퍼

### OCPP 메시지 흐름

1. `Socket.java`가 원시 WebSocket 프레임 수신
2. `SocketReceiveMessage.java`가 OCPP 배열 `[messageType, messageId, action, payload]` 파싱
3. `CALL (2)` 인 경우: `handlerreceive/`의 해당 `OcppHandler`로 디스패치
4. `CALLRESULT (3)` 인 경우: `messageId`로 대기 중인 요청과 매칭
5. 발신 메시지: `handlersend/`의 핸들러 스레드가 `SendHashMapObject` → `Socket.java` 호출

### SSL/TLS

`app/src/main/res/raw/`의 BKS 키스토어 파일:
- `charging_station_keystore.bks` (비밀번호: `ecospass`)
- `charging_station_truststore.bks` (비밀번호: `trustpass`)

### 리액티브/비동기 패턴

- **RxJava 3**: FTP 다운로드(`FtpRxJava.java`) 및 HTTP 작업
- **Android Handler**: 메인 스레드로 결과 전달
- **리스너/콜백 인터페이스**: ControlBoard, RfCard, Socket 이벤트 처리
- **백그라운드 스레드** (코루틴 아님): OCPP 하트비트 및 주기적 전송 루프

---

## V-CAT 결제 연동 (스마트로)

### 구조

```
MainActivity
    └─ VCatManager (vcat/VCatManager.java)       ← AIDL 바인딩 관리
           └─ SmartroVCatInterface (AIDL)         ← V-CAT 앱 서비스 (IPC)
                   └─ SmartroVCatCallback         ← 결과 수신 콜백

ClassUiProcess (basefunction/ClassUiProcess.java)
    └─ VCatListener (vcat/VCatListener.java)      ← 콜백 구현체 (채널별 등록)
           ├─ onServiceConnectionChanged()        ← 연결 끊기면 wakeUp() → bind() 재시도
           ├─ onEvent()                           ← 중간 이벤트 로그
           └─ onResult()                          ← 결제 결과 파싱 → ChargingCurrentData 저장 → PLUG_CHECK 전환

CreditCardFragment (pages/CreditCardFragment.java)
    └─ onViewCreated()에서 VCatManager.executeService() 호출 → 결제 요청 전송
```

### 관련 파일

| 파일 | 역할 |
|---|---|
| `vcat/VCatManager.java` | V-CAT 서비스 바인딩·요청·콜백 관리 |
| `vcat/VCatListener.java` | 이벤트/결과/연결상태 수신 인터페이스 |
| `basefunction/ClassUiProcess.java` | `VCatListener` 구현 및 채널 판별 로직 보유 |
| `pages/CreditCardFragment.java` | 결제 요청 JSON 생성 및 `executeService()` 호출 |
| `aidl/.../SmartroVCatInterface.aidl` | executeService / postExtraData / cancelService |
| `aidl/.../SmartroVCatCallback.aidl` | onServiceEvent / onServiceResult |

### MainActivity 초기화 순서

`VCatManager`는 `ClassUiProcess` 생성 **전에** 초기화해야 합니다. `ClassUiProcess` 생성자에서 리스너를 등록하기 때문입니다.

```java
// onCreate() — 7단계: VCatManager (ClassUiProcess보다 먼저)
vCatManager = new VCatManager(this);
boolean ok = vCatManager.bind();
if (!ok) {
    vCatManager.wakeUp();   // V-CAT 앱 강제 실행 후
    vCatManager.bind();     // 재바인딩
}

// onCreate() — 8단계: ClassUiProcess (생성자 안에서 vcat.setListener() 호출)
classUiProcess = new ClassUiProcess[GlobalVariables.maxChannel];
for (int i = 0; i < GlobalVariables.maxChannel; i++) {
    classUiProcess[i] = new ClassUiProcess(i);
}

// onDestroy()
vCatManager.unbind();
```

### VCatListener 구현 위치: ClassUiProcess

`ClassUiProcess` 생성자에서 리스너를 등록합니다. 채널이 2개이므로 두 번째 `ClassUiProcess[1]`의 등록이 앞의 것을 덮어씁니다. `onResult`에서 `CREDIT_CARD_WAIT` 상태인 채널을 스캔해 올바른 채널을 찾습니다 (`RfCardReaderListener`와 동일한 패턴).

```java
// ClassUiProcess 생성자 내부
VCatManager vcat = ((MainActivity) MainActivity.mContext).getVCatManager();
if (vcat != null) {
    vcat.setListener(vCatListener);
}

// vCatListener 필드
private final VCatListener vCatListener = new VCatListener() {
    @Override
    public void onServiceConnectionChanged(boolean connected) {
        if (!connected) { vcat.wakeUp(); vcat.bind(); }
    }
    @Override
    public void onEvent(String eventJson) { /* 로그 출력 */ }
    @Override
    public void onResult(String resultJson) {
        // 1. CREDIT_CARD_WAIT 채널 스캔
        // 2. errcode == "0000" 이면 ChargingCurrentData에 승인 정보 저장
        //    (authno, trandate, trantime, cardno, tid, merno, tran_serial, stlinst, reqinst)
        // 3. handler.post()로 메인 스레드에서 PLUG_CHECK 전환
        // 4. 실패 시 onHome() 호출
    }
};
```

### 결제 요청 흐름 (CreditCardFragment)

`CreditCardFragment.onViewCreated()`에서 V-CAT 결제를 시작합니다.

```java
int amount = GlobalVariables.FullRechgAmt;  // 완충 기준 금액 (int, 기본 40000)
int tax    = Math.round(amount / 11.0f);    // 부가세

JSONObject req = new JSONObject();
req.put("trantype", "0200");   // 일반 승인
req.put("tamt",     amount);
req.put("taxamt",   tax);
req.put("svcamt",   0);
req.put("halbu",    "00");     // 일시불

boolean sent = activity.getVCatManager().executeService(req.toString());
if (sent) {
    // UiSeq.CREDIT_CARD_WAIT Fragment로 전환 (대기 화면)
}
```

타이머 만료 시 `cancelService()` 호출 후 `onHome()`으로 복귀합니다.

### 결제 결과 필드 (onResult JSON, VCAT API V3.13 기준 추정)

| 필드 | 설명 | ChargingCurrentData setter |
|---|---|---|
| `errcode` | 결과 코드 (`0000` = 성공) | — |
| `authno` | 승인번호 | `setApprovalNumber()` |
| `trandate` | 거래일자 | `setApprovalDate()` |
| `trantime` | 거래시각 | `setApprovalTime()` |
| `cardno` | 카드번호 | `setCreditCardNumber()` |
| `tid` | 단말기 거래 ID | `setPgTranSeq()` |
| `merno` | 가맹점번호 | `setStoreNumber()` |
| `tran_serial` | 거래 일련번호 | `setTerminalNumber()` |
| `stlinst` | 발급사명 | `setIssuer()` |
| `reqinst` | 매입사명 | `setBuyer()` |

### 주요 메서드

| 메서드 | 설명 |
|---|---|
| `bind()` | V-CAT 서비스 바인딩. `false` 반환 시 `wakeUp()` 후 재시도 |
| `unbind()` | 서비스 해제 (onDestroy 필수 호출) |
| `executeService(json)` | 결제·취소 요청 전송 |
| `postExtraData(json)` | 서명·DCC 통화 선택 등 추가 데이터 전달 |
| `cancelService()` | 진행 중인 세션 강제 종료 (onServiceResult 수신 후 다음 거래 가능) |
| `wakeUp()` | V-CAT 앱 강제 실행 (Intent: `smartroapp://vcatscheme?manage=awake`) |
| `isBound()` | 바인딩 및 인터페이스 연결 여부 확인 |

### 미구현 항목 (TODO)

- OCPP `DataTransfer` (`PaymentData`) 서버 전송 연동 — 결제 승인 후 서버에 결제 정보 전달 필요
- `onResult` 필드명 VCAT API V3.13 문서로 최종 검증 필요 (현재 추정값 사용)
