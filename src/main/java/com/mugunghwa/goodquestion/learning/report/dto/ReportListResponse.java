package com.mugunghwa.goodquestion.learning.report.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReportListResponse(UUID reportId, UUID sessionId, String storyTitle,
                                 OffsetDateTime createdAt) {
}
