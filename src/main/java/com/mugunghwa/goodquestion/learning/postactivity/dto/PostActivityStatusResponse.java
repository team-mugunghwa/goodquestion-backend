package com.mugunghwa.goodquestion.learning.postactivity.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** 새로고침 복구용 상태 조회(활동-08). */
public record PostActivityStatusResponse(
        String status,
        List<PostActivityStartResponse.Card> cards,
        short attemptCount,
        Boolean isOrderCorrect,
        List<String> retellingKeywords,
        String retellingText,
        OffsetDateTime completedAt
) {
}
