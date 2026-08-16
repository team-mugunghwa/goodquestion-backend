package com.mugunghwa.goodquestion.ai.tts;

/** 실시간 합성 벤더. app_settings(tts.vendor)로 재배포 없이 전환한다. */
public enum TtsVendor {
    /** gpt-4o-mini-tts. 키만 있으면 항상 가용 - 기본값이자 최후 폴백 */
    OPENAI,
    /** gemini-2.5-flash-preview-tts. 사전 렌더 자산과 같은 화자(Leda/Puck/Charon/Kore) */
    GEMINI,
    /**
     * Google Cloud TTS Chirp 3: HD. 월 100만 자 무료라 테스트 기간용.
     * 보이스 페르소나 이름이 Gemini와 같아(ko-KR-Chirp3-HD-Leda 등) 화자 느낌이 유지된다.
     */
    CHIRP3
}
