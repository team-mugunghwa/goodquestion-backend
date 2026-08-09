package com.mugunghwa.goodquestion.story.content.dto;

import java.util.List;
import java.util.UUID;

/** 명세 3-5 이야기 카드 — 목록과 홈 추천이 공유한다. */
public record StoryCardResponse(
        UUID id,
        String title,
        String summary,
        String difficulty,
        Short estimatedMinutes,
        String imageUrl,
        List<String> topics
) {
}
