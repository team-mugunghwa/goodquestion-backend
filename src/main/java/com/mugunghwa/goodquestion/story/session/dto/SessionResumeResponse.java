package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.story.content.dto.SceneContentResponse;
import com.mugunghwa.goodquestion.story.mission.dto.MissionResponse;

import java.util.List;

/** 이어하기 복원 — 장면·대화 내역·마지막 대사·노출 중이던 미션을 한 번에 돌려준다(홈-01~02). */
public record SessionResumeResponse(
        SessionResponse session,
        SceneContentResponse currentScene,
        List<MessageResponse> messages,
        CharacterMessageResponse lastCharacterMessage,
        MissionResponse exposedMission
) {
}
