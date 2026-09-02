package com.dongah.fastcharger.smartro;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import service.vcat.smartro.com.vcat.SmartroVCatCallback;
import service.vcat.smartro.com.vcat.SmartroVCatInterface;

/**
 * SmtB420CardReader
 *
 * SMT-B420 블루투스 리더기를 통해 결제 없이 RF 카드 번호만 읽는 클래스.
 * V-CAT API의 getting-data: "card-no-via-device" 기능을 사용합니다.
 *
 * ── 전제 조건 ────────────────────────────────────────────────────
 *  - VCatPaymentManager.isSetupDone() == true (초기 설정 완료)
 *  - 스마트로 담당자에게 card-no-via-device 기능 활성화 요청 필요
 *
 * ── 사용 예시 ────────────────────────────────────────────────────
 *  SmtB420CardReader.getInstance().read(new SmtB420CardReader.CardReadCallback() {
 *      @Override
 *      public void onCardRead(String cardNo) {
 *          // 읽힌 카드 번호로 회원 조회 등 처리
 *      }
 *      @Override
 *      public void onError(String reason) {
 *          // 오류 처리
 *      }
 *      @Override
 *      public void onTimeout() {
 *          // 타임아웃 처리
 *      }
 *  });
 *
 *  // 카드 대기 중 취소 시
 *  SmtB420CardReader.getInstance().cancel();
 *
 * ── 카드 번호 형태 ────────────────────────────────────────────────
 *  V-CAT 응답: 마스킹된 카드 번호 (예: 123456******9000)
 *  회원카드 식별에는 앞 6자리(BIN) 또는 뒤 4자리를 활용하세요.
 *  전체 카드 번호는 보안상 V-CAT이 마스킹 처리합니다.
 */
public class SmtB420CardReader {
    private static final Logger logger = LoggerFactory.getLogger(SmtB420CardReader.class);

    private static final int DEFAULT_TIMEOUT_MS = 30_000;

    private static volatile SmtB420CardReader sInstance;

    public static SmtB420CardReader getInstance() {
        if (sInstance == null) {
            synchronized (SmtB420CardReader.class) {
                if (sInstance == null) sInstance = new SmtB420CardReader();
            }
        }
        return sInstance;
    }

    private SmtB420CardReader() {}

    private volatile boolean mReading = false;  // 카드 읽기 진행 중
    private CardReadCallback mCallback = null;
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());
    private Runnable mTimeoutRunnable = null;

    public interface CardReadCallback {
        /** 카드 읽기 성공 — cardNo: V-CAT이 반환한 마스킹 카드 번호 */
        void onCardRead(String cardNo);

        /** 카드 읽기 실패 */
        void onError(String reason);

        /** 타임아웃 (DEFAULT_TIMEOUT_MS 초과) */
        void onTimeout();
    }

    /**
     * SMT-B420 RF 카드 읽기 시작.
     * 기본 타임아웃(30초) 적용.
     *
     * @param callback 카드 읽기 결과 콜백
     */
    public void read(CardReadCallback callback) {
        read(callback, DEFAULT_TIMEOUT_MS);
    }

    /**
     * SMT-B420 RF 카드 읽기 시작.
     *
     * @param callback  카드 읽기 결과 콜백
     * @param timeoutMs 타임아웃 (밀리초)
     */
    public void read(CardReadCallback callback, int timeoutMs) {
        // ── 중복 호출 방지 ───────────────────────────────────────
        if (mReading) {
            logger.warn("SmtB420CardReader: 이미 카드 읽기 진행 중");
            if (callback != null) callback.onError("이미 카드 읽기가 진행 중입니다.");
            return;
        }

        // ── V-CAT 초기 설정 완료 여부 확인 ──────────────────────
        if (!VCatPaymentManager.getInstance().isSetupDone()) {
            logger.error("SmtB420CardReader: V-CAT 초기 설정 미완료");
            if (callback != null) callback.onError("V-CAT 초기 설정이 완료되지 않았습니다.");
            return;
        }

        SmartroVCatInterface vCatInterface = VCatPaymentManager.getInstance().getVcatInterface();
        if (vCatInterface == null) {
            logger.error("SmtB420CardReader: vcatInterface null");
            if (callback != null) callback.onError("V-CAT 서비스에 연결되어 있지 않습니다.");
            return;
        }

        mReading  = true;
        mCallback = callback;

        // ── 타임아웃 설정 ────────────────────────────────────────
        scheduleTimeout(timeoutMs);

        // ── V-CAT 카드 읽기 요청 ─────────────────────────────────
        try {
            JSONObject req = new JSONObject();
            req.put("service",      "function");
            req.put("getting-data", "card-no-via-device");

            logger.info("SmtB420CardReader: 카드 읽기 요청 → {}", req);

            vCatInterface.executeService(req.toString(), new SmartroVCatCallback.Stub() {

                @Override
                public void onServiceEvent(String eventJson) {
                    // card-no-via-device 는 중간 이벤트 없이 바로 result 반환
                    logger.debug("SmtB420CardReader onServiceEvent: {}", eventJson);
                }

                @Override
                public void onServiceResult(String resultJson) {
                    logger.info("SmtB420CardReader onServiceResult: {}", resultJson);
                    mUiHandler.post(() -> handleResult(resultJson));
                }
            });

        } catch (Exception e) {
            logger.error("SmtB420CardReader: 카드 읽기 요청 예외 → {}", e.getMessage());
            finishWithError("카드 읽기 요청 중 오류: " + e.getMessage());
        }
    }

    /**
     * 카드 대기 중 강제 취소.
     * Fragment onDestroyView() 등에서 호출하세요.
     */
    public void cancel() {
        if (!mReading) return;
        logger.info("SmtB420CardReader: 카드 읽기 취소 요청");
        try {
            SmartroVCatInterface vCatInterface =
                    VCatPaymentManager.getInstance().getVcatInterface();
            if (vCatInterface != null) {
                vCatInterface.cancelService();
            }
        } catch (Exception e) {
            logger.warn("SmtB420CardReader: cancelService 예외 → {}", e.getMessage());
        } finally {
            cleanup();
        }
    }

    public boolean isReading() {
        return mReading;
    }

    /**
     * V-CAT 결과 파싱
     *
     * 성공 응답 예시:
     * {
     *   "service": "function",
     *   "getting-data": "card-no-via-device",
     *   "card-no": "123456******9000",   ← 마스킹 카드 번호
     *   "service-result": "0000"
     * }
     */
    private void handleResult(String resultJson) {
        cancelTimeout();

        if (!mReading) return;  // 이미 취소된 경우

        try {
            JSONObject result = new JSONObject(resultJson);
            String serviceResult = result.optString("service-result", "");

            if (!"000".equals(serviceResult)) {
                // V-CAT 서비스 오류
                String desc = result.optString("service-description", "카드 읽기 실패");
                logger.error("SmtB420CardReader: 서비스 오류 [{}] {}", serviceResult, desc);
                finishWithError(desc);
                return;
            }

            // 카드 번호 추출
            // card-no-via-device 응답에서 카드번호 키 확인
            String cardNo = result.optString("card-no", "");

            if (cardNo.isEmpty()) {
                // 응답은 성공이지만 카드번호가 없는 경우
                // → 기능 비활성화 가능성(스마트로 담당자 확인 필요)
                logger.warn("SmtB420CardReader: card-no 값이 비어있음 " +
                        "→ 스마트로 담당자에게 card-no-via-device 기능 활성화 확인 필요");
                finishWithError("카드 번호를 읽지 못했습니다. (기능 활성화 확인 필요)");
                return;
            }

            logger.info("SmtB420CardReader: 카드 읽기 성공 → {}", cardNo);
            finishWithSuccess(cardNo);
        } catch (Exception e) {
            logger.error("SmtB420CardReader: 결과 파싱 오류 → {}", e.getMessage());
            finishWithError("결과 처리 중 오류: " + e.getMessage());
        }
    }

    private void finishWithSuccess(String cardNo) {
        CardReadCallback cb = mCallback;
        cleanup();
        if (cb != null) cb.onCardRead(cardNo);
    }

    private void finishWithError(String reason) {
        CardReadCallback cb = mCallback;
        cleanup();
        if (cb != null) cb.onError(reason);
    }

    private void finishWithTimeout() {
        logger.warn("SmtB420CardReader: 카드 읽기 타임아웃");
        // 타임아웃 시 V-CAT cancelService 호출
        try {
            SmartroVCatInterface vcatInterface =
                    VCatPaymentManager.getInstance().getVcatInterface();
            if (vcatInterface != null) vcatInterface.cancelService();
        } catch (Exception e) {
            logger.warn("SmtB420CardReader: 타임아웃 cancelService 예외 → {}", e.getMessage());
        }
        CardReadCallback cb = mCallback;
        cleanup();
        if (cb != null) cb.onTimeout();
    }

    private void scheduleTimeout(int timeoutMs) {
        cancelTimeout();
        mTimeoutRunnable = this::finishWithTimeout;
        mUiHandler.postDelayed(mTimeoutRunnable, timeoutMs);
    }

    private void cancelTimeout() {
        if (mTimeoutRunnable != null) {
            mUiHandler.removeCallbacks(mTimeoutRunnable);
            mTimeoutRunnable = null;
        }
    }


    private void cleanup() {
        mReading  = false;
        mCallback = null;
        cancelTimeout();
    }
}
