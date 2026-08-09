package com.mugunghwa.goodquestion.story.mission.dto;

/** 미노출 상태에서는 mission이 null이며 404가 아니다(미션-02). */
public record CurrentMissionResponse(MissionResponse mission) {
}
