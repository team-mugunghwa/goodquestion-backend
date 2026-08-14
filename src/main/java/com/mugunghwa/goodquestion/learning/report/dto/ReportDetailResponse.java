package com.mugunghwa.goodquestion.learning.report.dto;

import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.learning.report.Competency;
import com.mugunghwa.goodquestion.learning.report.HomeGuide;
import com.mugunghwa.goodquestion.learning.report.RepresentativeUtterance;
import com.mugunghwa.goodquestion.learning.report.ReportItem;
import com.mugunghwa.goodquestion.learning.report.VocabularyAnalysis;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 리포트 상세 (리포트 요건 2~7절).
 *
 * <p>strengths·nextFocus는 요소 코드만이 아니라 {@link ReportItem}({@code element + comment})을
 * 그대로 내린다. 코드만 내리면 화면에 "REASON"만 뜨고 왜 잘했는지가 사라진다.
 *
 * <p>vocabulary·competencies·representativeUtterance·homeGuide는 분석 본문이 아직 없으면
 * 비어 있다 — 요건이 일부 영역만 구성해도 성립한다고 열어 두었다.
 */
public record ReportDetailResponse(
        UUID id,
        UUID sessionId,
        String storyTitle,
        String summary,
        List<ReportItem> strengths,
        List<ReportItem> nextFocus,
        VocabularyAnalysis vocabulary,
        List<Competency> competencies,
        RepresentativeUtterance representativeUtterance,
        HomeGuide homeGuide,
        List<ElementEvidence> elementEvidences,
        OffsetDateTime createdAt
) {
    /**
     * 요소별 근거 발화 (요건 4절 "근거가 되는 실제 발화").
     *
     * <p>저장하지 않고 조회 시 분석 근거에서 매번 구성한다. 대표 발화 1건과는 다른 값이다 —
     * 이쪽은 역량마다 무엇을 보고 그렇게 판단했는지 보여주는 자리다.
     */
    public record ElementEvidence(String text, ThinkingElement element) {}
}