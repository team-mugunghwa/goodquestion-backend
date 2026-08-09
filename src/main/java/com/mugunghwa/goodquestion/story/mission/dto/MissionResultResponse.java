package com.mugunghwa.goodquestion.story.mission.dto;

/** 결과는 다음 턴 캐릭터 대사에 반영된다(미션-05, 미션-09). */
public record MissionResultResponse(String missionId, boolean accepted) {
}
