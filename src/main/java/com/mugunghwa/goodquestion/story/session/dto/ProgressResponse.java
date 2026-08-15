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
        /**
         * 이번 턴에 새로 인정된 요소. 프론트가 표정·반응을 고르는 데 쓴다
         * (충족조건 문서 §4의 상태 8종 매핑). 누적만으로는 "이번에 무엇이 통했는지"를
         * 알 수 없어 매 턴 같은 표정이 된다(08-15 요청 #9-4).
         */
        List<ThinkingElement> newElements,
        List<ThinkingElement> missingElements,
        int turnCount,
        int maxTurns,
        ThinkingElement guidanceTarget
) {
}
