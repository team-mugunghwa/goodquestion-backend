package com.mugunghwa.goodquestion.story.content.dto;

/**
 * 이야기 상세 — 카드 전체 필드에 상세 전용 값을 더한다(선택-03).
 */
public record StoryDetailResponse(
        StoryCardResponse story,
        int sceneCount,
        String childRole,
        String intro
) {
}
