package com.mugunghwa.goodquestion.infra.tts;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class DefaultTtsClient implements TtsClient {

    private final WebClient webClient;

    @Override
    public byte[] synthesize(String text) {
        throw new UnsupportedOperationException("TODO");
    }
}
