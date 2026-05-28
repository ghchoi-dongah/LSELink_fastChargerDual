package com.dongah.fastcharger.vcat;

public interface VCatListener {
    /** V-CAT 이벤트/결과/연결 상태 수신 콜백 */
    /* 진행 중인 상태 또는 사용자 입력 요청 (status/prompt) */
    void onEvent(String eventJson);
    /* 최종 결과 (승인/거절/오류) */
    void onResult(String resultJson);
    /* 서비스 연결 상태 변경 */
    void onServiceConnectionChanged(boolean connected);
}
