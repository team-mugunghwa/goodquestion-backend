package com.mugunghwa.goodquestion.story.mission.dto;

import com.mugunghwa.goodquestion.story.mission.MissionType;

import java.util.List;

/**
 * 명세 3-13 미션. payload는 유형에 따라 한쪽만 값이 있다 —
 * 미션1은 questions(4요소), 미션2는 cards(친구 카드 4종).
 */
public record MissionResponse(
        String missionId,
        MissionType missionType,
        String title,
        String description,
        Payload payload
) {
    public record Payload(List<Question> questions, List<Card> cards) {}

    /** key는 tool/reason/request/expectedResult로 고정한다(미션-04). */
    public record Question(String key, String label) {}

    public record Card(String key, String label, String imageUrl, String template) {}
}
