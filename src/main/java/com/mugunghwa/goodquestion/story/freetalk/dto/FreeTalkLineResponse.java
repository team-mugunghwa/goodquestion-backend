package com.mugunghwa.goodquestion.story.freetalk.dto;

import com.mugunghwa.goodquestion.global.vocab.CharacterEmotion;

/**
 * 캐릭터가 말한 한 줄.
 *
 * <p>audioUrl이 null이면 합성에 실패했다는 뜻이고, 클라이언트는 학습 대화와 똑같이
 * /api/tts로 직접 만든다 - 목소리가 안 나온다고 대화 자체를 막지는 않는다.
 */
public record FreeTalkLineResponse(String text, String audioUrl, CharacterEmotion emotion) {
}
