package com.mugunghwa.goodquestion.story.dialogue.dto;

import com.mugunghwa.goodquestion.story.mission.dto.MissionResponse;
import com.mugunghwa.goodquestion.story.session.dto.CharacterMessageResponse;
import com.mugunghwa.goodquestion.story.session.dto.MessageResponse;
import com.mugunghwa.goodquestion.story.session.dto.ProgressResponse;
import com.mugunghwa.goodquestion.story.session.dto.SceneTransitionResponse;

/**
 * 명세 4-6 발화 제출·턴 처리 응답.
 *
 * <p>단일 스키마이고 null 여부로 3분기한다.
 * 대화 계속(mission·sceneTransition 모두 null) / 미션 노출(mission 있음) /
 * 장면 종료(sceneTransition 있음, progress.mode=CLOSING).
 */
public record UtteranceResponse(
        MessageResponse childMessage,
        AnalysisResponse analysis,
        ProgressResponse progress,
        /** 일반·유도는 생성 대사, 종료는 고정 마지막 대사(캐릭터-12) */
        CharacterMessageResponse characterMessage,
        /** 값이 있으면 미션 오버레이를 노출한다 */
        MissionResponse mission,
        /** 장면이 끝날 때만 값이 있다 */
        SceneTransitionResponse sceneTransition,
        /**
         * 위험 신호로 대사 생성이 중단된 턴에만 값이 있다.
         * 값이 있으면 characterMessage는 생성 대사가 아니라 안전 문구다.
         */
        SafetyResponse safety
) {
}
