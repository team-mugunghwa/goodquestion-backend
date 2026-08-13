package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.story.session.Message;

import java.util.UUID;

/**
 * 명세 3-9 캐릭터 메시지. text는 "ㅇㅇ" 이름 치환이 끝난 상태다(캐릭터-17).
 * audioUrl이 null이면 클라이언트가 텍스트→음성 변환을 호출한다.
 */
public record CharacterMessageResponse(UUID messageId, String text, String audioUrl) {

    /** audioUrl은 TTS 미구현이라 null - 클라이언트가 /api/tts로 합성한다. */
    public static CharacterMessageResponse from(Message message) {
        return new CharacterMessageResponse(message.getId(), message.getText(), null);
    }
}
