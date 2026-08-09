package com.mugunghwa.goodquestion.ai.tts;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * TODO: 공급자 선정 후 구현(미결-01).
 * 합성 결과를 스토리지에 올리고 만료 있는 URL을 돌려주는 방식이라,
 * 공급자 선정과 함께 오디오 저장소도 정해야 한다.
 */
@Component
@RequiredArgsConstructor
public class DefaultTtsClient implements TtsClient {

    private final WebClient webClient;

    @Override
    public SynthesizedAudio synthesize(String text, String characterName) {
        throw new UnsupportedOperationException("미구현: TTS 합성 — 공급자·오디오 스토리지 선정 필요");
    }
}
