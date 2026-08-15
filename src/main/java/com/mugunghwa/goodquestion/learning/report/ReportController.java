package com.mugunghwa.goodquestion.learning.report;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.learning.report.dto.ReportDetailResponse;
import com.mugunghwa.goodquestion.learning.report.dto.ReportListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/api/children/{childId}/reports")
    public List<ReportListResponse> getReports(@CurrentParentId UUID parentId,
                                               @PathVariable UUID childId) {
        return reportService.getReports(parentId, childId);
    }

    /** 세션의 대화·분석을 집계해 리포트를 생성한다(리포트-01~04). */
    @PostMapping("/api/sessions/{sessionId}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportDetailResponse createReport(@CurrentParentId UUID parentId,
                                             @PathVariable UUID sessionId) {
        reportService.generateNow(parentId, sessionId);
        return reportService.getReport(parentId, sessionId);
    }

}
