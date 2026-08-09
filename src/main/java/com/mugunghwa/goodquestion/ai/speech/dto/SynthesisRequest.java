package com.mugunghwa.goodquestion.ai.speech.dto;

import java.util.UUID;

/** messageId 또는 text 중 하나 필수 */
public record SynthesisRequest(UUID messageId, String text) {
}
