package com.mugunghwa.goodquestion.story.session.dto;


/**
 * STORY 장면 재생 완료 → 다음 장면 이동 결과.
 * 다음 장면이 DIALOGUE면 openingMessage 포함(재생 시점 저장 원칙),
 * 마지막 장면이 STORY였다면 postActivity = true.
 */
public record SceneAdvanceResponse(
        SceneResponse nextScene,          // postActivity = true면 null
        MessageResponse openingMessage,   // 다음 장면이 STORY면 null
        boolean postActivity
) {
}
