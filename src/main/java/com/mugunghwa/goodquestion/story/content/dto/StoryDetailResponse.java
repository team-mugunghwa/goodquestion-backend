package com.mugunghwa.goodquestion.story.content.dto;

/**
 * 이야기 상세 — 카드 전체 필드에 상세 전용 값을 더한다(선택-03).
 *
 * <p>childRole·intro는 stories 테이블에 컬럼이 없어 항상 null이다.
 * TODO: 스키마에 child_role·intro 추가 후 채운다.
 */
public record StoryDetailResponse(
        StoryCardResponse story,
        int sceneCount,
        String childRole,
        String intro
) {
}
