package com.mugunghwa.goodquestion.learning.report;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.story.dialogue.DetectedElement;
import com.mugunghwa.goodquestion.story.dialogue.UtteranceAnalysis;
import com.mugunghwa.goodquestion.story.dialogue.UtteranceAnalysisRepository;
import com.mugunghwa.goodquestion.ai.report.ReportLlmClient;
import com.mugunghwa.goodquestion.learning.report.dto.ReportDetailResponse;
import com.mugunghwa.goodquestion.learning.report.dto.ReportListResponse;
import com.mugunghwa.goodquestion.story.session.SessionService;
import com.mugunghwa.goodquestion.story.session.StorySession;
import com.mugunghwa.goodquestion.user.child.ChildService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UtteranceAnalysisRepository analysisRepository;
    private final ReportLlmClient reportLlmClient;
    private final SessionService sessionService;
    private final ChildService childService;

    /** 세션 완료 시 PostActivityService에서 호출 (비동기) */
    @Async
    @Transactional
    public void generate(UUID sessionId) {
        // TODO: 세션의 messages + utterance_analyses 종합 → LLM으로 summary/strengths/nextFocus 생성 → 저장
        //  실패 시 재시도 정책 필요 (조회 시 REPORT_NOT_READY로 방어)
        throw new UnsupportedOperationException("TODO");
    }

    public List<ReportListResponse> getReports(UUID parentId, UUID childId) {
        childService.getOwnedChild(parentId, childId);

        return reportRepository.findAllByChildId(childId).stream()
                .map(report -> new ReportListResponse(
                        report.getId(),
                        report.getSession().getId(),
                        report.getSession().getStory().getTitle(),
                        report.getCreatedAt()))
                .toList();
    }

    /**
     * 세션 리포트 조회. 리포트는 후속 활동 완료 후 비동기로 생성되므로 아직 없을 수 있다.
     *
     * <p>없는 것을 404로 돌려주면 "세션이 없다"와 구분되지 않는다. 보호자 화면은 둘을
     * 다르게 안내해야 한다 — 하나는 잘못된 접근이고 하나는 잠시 뒤 다시 보면 되는 상태다.
     */
    public ReportDetailResponse getReport(UUID parentId, UUID sessionId) {
        StorySession session = sessionService.getOwnedSession(parentId, sessionId);
        Report report = reportRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_READY));

        return new ReportDetailResponse(
                report.getId(),
                sessionId,
                session.getStory().getTitle(),
                report.getSummary(),
                report.getStrengths(),
                report.getNextFocus(),
                representativeUtterances(sessionId),
                report.getCreatedAt());
    }

    /**
     * 대표 발화 구성 — 저장하지 않고 분석 근거에서 매번 만든다(리포트-04).
     *
     * <p>요소당 가장 이른 턴 하나만 남긴다. 같은 요소가 여러 턴에서 나오면 화면에 비슷한
     * 문장이 반복되고, 무엇을 잘했는지가 오히려 흐려진다.
     *
     * <p>인식이 미덥지 않았던 발화는 후보에서 뺀다. 저장된 원문이 아이가 실제로 한 말과
     * 다를 수 있는데, 리포트는 보호자에게 "아이가 이렇게 말했다"고 보여주는 자리다.
     */
    private List<ReportDetailResponse.RepresentativeUtterance> representativeUtterances(UUID sessionId) {
        Map<ThinkingElement, ReportDetailResponse.RepresentativeUtterance> byElement = new LinkedHashMap<>();

        for (UtteranceAnalysis analysis : analysisRepository.findAllBySessionId(sessionId)) {
            if (analysis.getMessage().isSttLowConfidence()) {
                continue;
            }
            for (DetectedElement detected : analysis.getDetectedElements()) {
                byElement.putIfAbsent(detected.type(),
                        new ReportDetailResponse.RepresentativeUtterance(
                                textOf(detected, analysis), detected.type()));
            }
        }

        return List.copyOf(byElement.values());
    }

    /** 근거는 발화에서 잘라낸 조각이라 그대로 쓴다. 비어 있으면 발화 전체로 대신한다. */
    private String textOf(DetectedElement detected, UtteranceAnalysis analysis) {
        return (detected.evidence() == null || detected.evidence().isBlank())
                ? analysis.getMessage().getText()
                : detected.evidence();
    }
}
