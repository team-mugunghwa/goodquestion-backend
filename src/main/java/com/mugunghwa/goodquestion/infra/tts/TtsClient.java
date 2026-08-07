package com.mugunghwa.goodquestion.infra.tts;

public interface TtsClient {

    /** @return 합성 음성 (audio/mpeg 바이트) — TODO: 캐릭터별 voice 파라미터 */
    byte[] synthesize(String text);
}
