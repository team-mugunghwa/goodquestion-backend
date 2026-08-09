package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.global.vocab.ResponseMode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;

import java.util.List;

/**
 * 명세 3-11 진행 상태.
 * missingElements는 저장하지 않고 (장면 목표 요소 − 누적 요소)로 매번 계산한다(진행-04).
 */
public record ProgressResponse(
        ResponseMode mode,
        List<ThinkingElement> accumulatedElements,
        List<ThinkingElement> missingElements,
        int turnCount,
        int maxTurns,
        ThinkingElement guidanceTarget
) {
}
