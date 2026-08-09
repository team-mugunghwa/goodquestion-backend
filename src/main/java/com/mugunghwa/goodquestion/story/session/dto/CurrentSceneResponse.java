package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.story.content.dto.SceneContentResponse;
import com.mugunghwa.goodquestion.story.session.PlayPhase;

/** 새로고침·화면 전환 시 현재 장면을 다시 그리기 위한 응답. */
public record CurrentSceneResponse(SceneContentResponse currentScene, PlayPhase phase) {
}
