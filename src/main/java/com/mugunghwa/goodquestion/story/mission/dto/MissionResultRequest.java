package com.mugunghwa.goodquestion.story.mission.dto;

import java.util.List;
import java.util.Map;

/** 미션 유형에 따라 한쪽만 사용한다 — 미션1은 answers, 미션2는 cards. */
public record MissionResultRequest(
        Map<String, String> answers,
        List<CardAnswer> cards
) {
    public record CardAnswer(String key, String strengthText) {}
}
