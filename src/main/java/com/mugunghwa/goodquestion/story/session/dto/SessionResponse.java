package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.story.session.SessionStatus;

import java.util.UUID;

public record SessionResponse(
        UUID sessionId,
        SessionStatus status,
        SceneResponse currentScene,
        short currentChildTurnCount,
        MessageResponse openingMessage   // 세션 시작 응답에만 포함, 조회 시 null
) {
}
