package com.mugunghwa.goodquestion.session.session.dto;

import com.mugunghwa.goodquestion.session.message.dto.MessageResponse;
import com.mugunghwa.goodquestion.session.session.SessionStatus;

import java.util.UUID;

public record SessionResponse(
        UUID sessionId,
        SessionStatus status,
        SceneResponse currentScene,
        short currentChildTurnCount,
        MessageResponse openingMessage   // 세션 시작 응답에만 포함, 조회 시 null
) {
}
