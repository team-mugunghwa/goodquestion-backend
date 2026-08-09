package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.global.vocab.CharacterEmotion;
import com.mugunghwa.goodquestion.story.session.Message;
import com.mugunghwa.goodquestion.story.session.SpeakerType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(UUID messageId, SpeakerType speakerType, int turnOrder,
                              String text, CharacterEmotion characterEmotion,
                              OffsetDateTime createdAt) {

    public static MessageResponse from(Message m) {
        return new MessageResponse(m.getId(), m.getSpeakerType(), m.getTurnOrder(),
                m.getText(), m.getCharacterEmotion(), m.getCreatedAt());
    }
}
