package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.story.session.SceneEndReason;
import com.mugunghwa.goodquestion.story.session.SceneTransitionTarget;
import com.mugunghwa.goodquestion.story.content.SceneType;

import java.util.UUID;

/** 명세 3-12 장면 전환 — 턴 처리에서 장면이 종료될 때만 값이 있다. */
public record SceneTransitionResponse(
        SceneTransitionTarget next,
        UUID nextSceneId,
        Integer nextSceneOrder,
        SceneType nextSceneType,
        SceneEndReason closingReason
) {
}
