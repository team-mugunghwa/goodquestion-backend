package com.mugunghwa.goodquestion.learning.report.dto;

import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** 명세 4-10 리포트 — 전체 요약 / 잘 보여준 요소 / 다음 연습 요소 + 대표 발화(리포트-02, 04). */
public record ReportDetailResponse(
        UUID id,
        UUID sessionId,
        String storyTitle,
        String summary,
        List<ThinkingElement> strengths,
        List<ThinkingElement> nextFocus,
        List<RepresentativeUtterance> representativeUtterances,
        OffsetDateTime createdAt
) {
    /** 저장값이 아니라 조회 시 분석 근거(evidence)와 messages에서 구성한다. */
    public record RepresentativeUtterance(String text, ThinkingElement element) {}
}
