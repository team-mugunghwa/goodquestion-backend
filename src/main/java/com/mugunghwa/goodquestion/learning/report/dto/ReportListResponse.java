package com.mugunghwa.goodquestion.learning.report.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 명세 4-10 리포트 목록. */
public record ReportListResponse(UUID id, UUID sessionId, String storyTitle, OffsetDateTime createdAt) {
}
