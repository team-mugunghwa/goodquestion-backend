package com.mugunghwa.goodquestion.ai.speech.dto;

import java.time.OffsetDateTime;

/**
 * 텍스트 → 음성 응답.
 * 바이트를 직접 내리지 않고 URL을 주어 클라이언트가 다시 듣기·캐싱을 할 수 있게 한다.
 */
public record SynthesisResponse(String audioUrl, OffsetDateTime expiresAt) {
}
