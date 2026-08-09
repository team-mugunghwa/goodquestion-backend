package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.story.content.SceneType;
import com.mugunghwa.goodquestion.story.session.SessionStatus;
import com.mugunghwa.goodquestion.story.session.PlayPhase;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 명세 4-4 세션 상태 조회.
 * 여기서는 장면 콘텐츠 전체가 아니라 식별 정보만 준다 — 렌더는 현재 장면 조회를 쓴다.
 */
public record SessionResponse(
        UUID sessionId,
        UUID childId,
        UUID storyId,
        SessionStatus status,
        SceneRef currentScene,
        PlayPhase phase,
        ProgressResponse progress,
        boolean sceneGoalMet,
        OffsetDateTime lastActivityAt
) {
    public record SceneRef(UUID sceneId, Short sceneOrder, SceneType sceneType) {}
}
