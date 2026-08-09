package com.mugunghwa.goodquestion.story.content.dto;

import com.mugunghwa.goodquestion.story.content.SceneType;
import com.mugunghwa.goodquestion.story.content.StoryScene;

import java.util.UUID;

/**
 * 클라이언트 장면 재생용 콘텐츠.
 * element_criteria, remaining_worries, mission_config(노출 조건) 등
 * 서버 내부 설정은 의도적으로 제외한다 — 미션은 노출 시점에 /utterances 응답으로 전달.
 */
public record SceneContentResponse(
        UUID sceneId,
        short sceneOrder,
        SceneType sceneType,
        String sceneDescription,   // STORY: 내레이션 본문 / DIALOGUE: 상황 설명
        String imageUrl,
        String characterName,      // STORY면 null
        boolean hasMission
) {
    public static SceneContentResponse from(StoryScene s) {
        return new SceneContentResponse(s.getId(), s.getSceneOrder(), s.getSceneType(),
                s.getSceneDescription(), s.getImageUrl(), s.getCharacterName(), s.hasMission());
    }
}
