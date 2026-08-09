package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.story.session.SessionStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 명세 3-6 세션 요약 — 홈의 이어하기 카드에 쓴다(홈-01~02). */
public record SessionSummaryResponse(
        UUID sessionId,
        UUID storyId,
        String storyTitle,
        String storyImageUrl,
        SessionStatus status,
        short currentSceneOrder,
        int totalScenes,
        OffsetDateTime lastActivityAt
) {
}
