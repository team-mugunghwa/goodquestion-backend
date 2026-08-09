package com.mugunghwa.goodquestion.story.dialogue.dto;

import com.mugunghwa.goodquestion.global.vocab.ChildIntent;
import com.mugunghwa.goodquestion.global.vocab.UtteranceValidity;
import com.mugunghwa.goodquestion.story.dialogue.DetectedElement;
import com.mugunghwa.goodquestion.story.dialogue.UtteranceAnalysis;

import java.util.List;

/**
 * 명세 3-10 분석 결과.
 *
 * <p>캐릭터 표정·태도 변화의 트리거를 겸한다 — 프론트가 자체 판단하지 않고
 * 이 값으로만 연출한다(마음-01). 분석 LLM 출력 4가지 외에는 담지 않는다(분석-05).
 */
public record AnalysisResponse(
        ChildIntent childIntent,
        String mainPoint,
        List<DetectedElement> detectedElements,
        UtteranceValidity utteranceValidity
) {

    public static AnalysisResponse from(UtteranceAnalysis analysis) {
        return new AnalysisResponse(
                analysis.getChildIntent(),
                analysis.getMainPoint(),
                analysis.getDetectedElements(),
                analysis.getUtteranceValidity());
    }
}
