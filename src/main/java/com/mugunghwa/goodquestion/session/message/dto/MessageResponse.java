package com.mugunghwa.goodquestion.session.message.dto;

import com.mugunghwa.goodquestion.session.message.CharacterEmotion;
import com.mugunghwa.goodquestion.session.message.Message;
import com.mugunghwa.goodquestion.session.message.SpeakerType;

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
