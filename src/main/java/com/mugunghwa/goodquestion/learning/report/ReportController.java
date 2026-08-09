package com.mugunghwa.goodquestion.learning.report;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.learning.report.dto.ReportDetailResponse;
import com.mugunghwa.goodquestion.learning.report.dto.ReportListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/api/children/{childId}/reports")
    public Page<ReportListResponse> getReports(@CurrentParentId UUID parentId,
                                               @PathVariable UUID childId,
                                               @PageableDefault(size = 10) Pageable pageable) {
        return reportService.getReports(parentId, childId, pageable);
    }

    @GetMapping("/api/sessions/{sessionId}/report")
    public ReportDetailResponse getReport(@CurrentParentId UUID parentId,
                                          @PathVariable UUID sessionId) {
        return reportService.getReport(parentId, sessionId);
    }

    /**
     * 세션의 대화·분석을 집계해 리포트를 생성한다(리포트-01~04).
     * TODO: ReportService 생성 메서드 구현 — 현재는 조회만 있다.
     */
    @PostMapping("/api/sessions/{sessionId}/report")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportDetailResponse createReport(@CurrentParentId UUID parentId,
                                             @PathVariable UUID sessionId) {
        throw new UnsupportedOperationException("미구현: 리포트 생성");
    }
}
