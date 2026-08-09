package com.mugunghwa.goodquestion.ai.tts;

import java.time.OffsetDateTime;

/** TTS 어댑터 산출물 — 스토리지에 올린 오디오의 접근 URL과 만료 시각. */
public record SynthesizedAudio(String audioUrl, OffsetDateTime expiresAt) {
}
