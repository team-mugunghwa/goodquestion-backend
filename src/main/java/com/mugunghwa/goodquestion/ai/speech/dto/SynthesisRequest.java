package com.mugunghwa.goodquestion.ai.speech.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 텍스트 → 음성 요청.
 * characterName이 있으면 캐릭터 보이스로, 없으면 내레이션 보이스로 합성한다(장면-03~04, 단어-03).
 */
public record SynthesisRequest(@NotBlank String text, String characterName) {
}
