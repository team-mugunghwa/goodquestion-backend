package com.mugunghwa.goodquestion.learning.report.dto;

import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ReportDetailResponse(
        UUID reportId, String storyTitle, String summary,
        List<Item> strengths, List<Item> nextFocus, OffsetDateTime createdAt
) {
    /** representativeUtterance는 저장값이 아니라 조회 시 evidence·messages에서 구성 */
    public record Item(ThinkingElement element, String comment, String representativeUtterance) {}
}
