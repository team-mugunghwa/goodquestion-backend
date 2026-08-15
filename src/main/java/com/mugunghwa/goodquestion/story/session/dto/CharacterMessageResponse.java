package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.story.session.Message;

import java.util.UUID;

/**
 * 명세 3-9 캐릭터 메시지. text는 "ㅇㅇ" 이름 치환이 끝난 상태다(캐릭터-17).
 * audioUrl이 null이면 클라이언트가 텍스트→음성 변환을 호출한다.
 */
public record CharacterMessageResponse(UUID messageId, String text, String audioUrl) {

    /**
     * 사전 렌더 음성이 없는 메시지 — LLM이 그때그때 만든 대사는 미리 렌더할 수 없다.
     * 클라이언트가 {@code /api/tts}로 합성한다.
     */
    public static CharacterMessageResponse from(Message message) {
        return from(message, null);
    }

    /**
     * @param audioUrl 이 문장으로 렌더된 음성의 URL. null이면 클라이언트가 합성한다.
     *                 {@code SceneAudioResolver}가 문장 해시로 찾아 준 값만 넣어야 한다 —
     *                 슬롯만 보고 넣으면 대사를 고쳤을 때 옛 음성이 그대로 나간다.
     */
    public static CharacterMessageResponse from(Message message, String audioUrl) {
        return new CharacterMessageResponse(message.getId(), message.getText(), audioUrl);
    }
}
