package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.global.vocab.CharacterEmotion;
import com.mugunghwa.goodquestion.story.session.Message;
import com.mugunghwa.goodquestion.story.session.SpeakerType;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 대화 기록 1건.
 *
 * <p>sttLowConfidence는 아이 발화에만 의미가 있다. 인식이 미덥지 않았음을 화면이 알 수 있어야
 * 다시 말하기를 안내할 수 있다 — 신뢰도 원값은 내부 지표라 내리지 않는다.
 */
public record MessageResponse(UUID messageId, SpeakerType speakerType, int turnOrder,
                              String text, boolean sttLowConfidence,
                              CharacterEmotion characterEmotion,
                              OffsetDateTime createdAt) {

    public static MessageResponse from(Message m) {
        return new MessageResponse(m.getId(), m.getSpeakerType(), m.getTurnOrder(),
                m.getText(), m.isSttLowConfidence(), m.getCharacterEmotion(), m.getCreatedAt());
    }
}
