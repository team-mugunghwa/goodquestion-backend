package com.mugunghwa.goodquestion.learning.report;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.learning.report.dto.AxisScoreResponse;
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
    private final AxisScoreService axisScoreService;

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

    /** 세션별 리포트 상세. 생성 전이면 REPORT_NOT_READY(409). */
    @GetMapping("/api/sessions/{sessionId}/report")
    public ReportDetailResponse getReport(@CurrentParentId UUID parentId,
                                          @PathVariable UUID sessionId) {
        return reportService.getReport(parentId, sessionId);
    }

    /**
     * 6각 그래프 축 점수 — {@code reports} 행(요약·강점·다음 연습, LLM 생성) 유무와 무관하게
     * story_scenes + utterance_analyses만 집계해 내려준다. ReportService.generate()가
     * LLM 벤더 미정으로 비어 있는 동안에도 이 경로는 독립적으로 동작한다.
     */
    @GetMapping("/api/sessions/{sessionId}/report/axis-scores")
    public List<AxisScoreResponse> getAxisScores(@CurrentParentId UUID parentId,
                                                 @PathVariable UUID sessionId) {
        return axisScoreService.getAxisScores(parentId, sessionId);
    }
}
