package com.mugunghwa.goodquestion.learning.report;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.learning.report.dto.ReportDetailResponse;
import com.mugunghwa.goodquestion.learning.report.dto.ReportListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/api/v1/children/{childId}/reports")
    public Page<ReportListResponse> getReports(@CurrentParentId UUID parentId,
                                               @PathVariable UUID childId,
                                               @PageableDefault(size = 10) Pageable pageable) {
        return reportService.getReports(parentId, childId, pageable);
    }

    @GetMapping("/api/v1/sessions/{sessionId}/report")
    public ReportDetailResponse getReport(@CurrentParentId UUID parentId,
                                          @PathVariable UUID sessionId) {
        return reportService.getReport(parentId, sessionId);
    }
}
