package com.mugunghwa.goodquestion.learning.report;

import com.mugunghwa.goodquestion.dialog.analysis.UtteranceAnalysisRepository;
import com.mugunghwa.goodquestion.infra.llm.ReportLlmClient;
import com.mugunghwa.goodquestion.learning.report.dto.ReportDetailResponse;
import com.mugunghwa.goodquestion.learning.report.dto.ReportListResponse;
import com.mugunghwa.goodquestion.session.session.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UtteranceAnalysisRepository analysisRepository;
    private final ReportLlmClient reportLlmClient;
    private final SessionService sessionService;

    /** 세션 완료 시 ActivityService에서 호출 (비동기) */
    @Async
    @Transactional
    public void generate(UUID sessionId) {
        // TODO: 세션의 messages + utterance_analyses 종합 → LLM으로 summary/strengths/nextFocus 생성 → 저장
        //  실패 시 재시도 정책 필요 (조회 시 REPORT_NOT_READY로 방어)
        throw new UnsupportedOperationException("TODO");
    }

    public Page<ReportListResponse> getReports(UUID parentId, UUID childId, Pageable pageable) {
        // TODO: 아이 소유권 검증 → 목록 매핑
        throw new UnsupportedOperationException("TODO");
    }

    public ReportDetailResponse getReport(UUID parentId, UUID sessionId) {
        sessionService.getOwnedSession(parentId, sessionId);
        // TODO: 없으면 REPORT_NOT_READY(409) → 있으면 대표 발화(evidence) 구성해 반환
        throw new UnsupportedOperationException("TODO");
    }
}
