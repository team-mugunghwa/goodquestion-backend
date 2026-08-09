package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.story.content.dto.SceneContentResponse;
import com.mugunghwa.goodquestion.story.session.PlayPhase;
import com.mugunghwa.goodquestion.story.session.SessionStatus;

import java.util.UUID;

/** 세션 시작 — 도입 장면을 즉시 렌더할 수 있도록 콘텐츠 전체를 함께 준다. */
public record SessionStartResponse(
        UUID sessionId,
        SessionStatus status,
        SceneContentResponse currentScene,
        PlayPhase phase
) {
}
