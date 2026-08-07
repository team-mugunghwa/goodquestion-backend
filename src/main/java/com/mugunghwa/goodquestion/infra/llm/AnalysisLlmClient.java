package com.mugunghwa.goodquestion.infra.llm;

import org.springframework.stereotype.Component;

/**
 * 발화 분석 LLM 호출 (발화 분석 문서 4·6장 입출력 명세).
 * 입력: sceneContext, goal, previousCharacterMessage, childUtterance, targetElements, elementCriteria
 * 출력: childIntent, detectedElements[{type, evidence}], utteranceValidity (+ mainPoint)
 */
@Component
public class AnalysisLlmClient {

    public AnalysisLlmResult analyze(AnalysisLlmInput input) {
        // TODO: AnalysisPromptBuilder로 프롬프트 조립 → LLM 호출 → JSON 파싱
        throw new UnsupportedOperationException("TODO");
    }

    public record AnalysisLlmInput(String sceneContext, String goal, String previousCharacterMessage,
                                   String childUtterance, java.util.List<String> targetElements,
                                   java.util.Map<String, String> elementCriteria) {}

    public record AnalysisLlmResult(String childIntent, String mainPoint,
                                    java.util.List<DetectedElementDto> detectedElements,
                                    String utteranceValidity) {
        public record DetectedElementDto(String type, String evidence) {}
    }
}
