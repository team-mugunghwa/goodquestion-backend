package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.story.session.SceneEndReason;
import com.mugunghwa.goodquestion.story.session.SceneTransitionTarget;
import com.mugunghwa.goodquestion.story.content.SceneType;

import java.util.UUID;

/**
 * 명세 3-12 장면 전환 — 턴 처리에서 장면이 종료될 때만 값이 있다.
 *
 * @param resultImageUrl 끝난 장면의 결과 연출 이미지(2026-08 확정). 값이 있으면 마지막 대사
 *                       재생 뒤, 다음 장면을 그리기 전에 이 이미지를 연출로 끼워 넣는다.
 *                       대화3의 "배가 떨어지는 결과 연출"이 여기 해당하며, 종료 사유나
 *                       미션 노출 여부와 무관하게 장면이 닫히면 항상 내려간다
 */
public record SceneTransitionResponse(
        SceneTransitionTarget next,
        UUID nextSceneId,
        Integer nextSceneOrder,
        SceneType nextSceneType,
        SceneEndReason closingReason,
        String resultImageUrl
) {
}
