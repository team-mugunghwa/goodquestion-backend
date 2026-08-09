package com.mugunghwa.goodquestion.ai.stt;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class DefaultSttClient implements SttClient {

    private final WebClient webClient;

    @Override
    public String transcribe(MultipartFile audio) {
        // TODO: 선택한 STT 벤더 API 호출 (한국어·아동 발화 인식률 검증 필요)
        throw new UnsupportedOperationException("TODO");
    }
}
