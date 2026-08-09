package com.mugunghwa.goodquestion.learning.report.dto;

import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.learning.report.ReportItem;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 명세 4-10 리포트 — 전체 요약 / 잘 보여준 요소 / 다음 연습 요소 + 대표 발화(리포트-02, 04).
 *
 * <p>strengths·nextFocus는 요소 코드만이 아니라 {@link ReportItem}({@code element + comment})을
 * 그대로 내린다. 보호자 화면에 보여줄 문장이 comment에 들어 있어, 코드만 내리면
 * "REASON"만 뜨고 왜 잘했는지가 사라진다.
 */
public record ReportDetailResponse(
        UUID id,
        UUID sessionId,
        String storyTitle,
        String summary,
        List<ReportItem> strengths,
        List<ReportItem> nextFocus,
        List<RepresentativeUtterance> representativeUtterances,
        OffsetDateTime createdAt
) {
    /** 저장값이 아니라 조회 시 분석 근거(evidence)와 messages에서 구성한다. */
    public record RepresentativeUtterance(String text, ThinkingElement element) {}
}
