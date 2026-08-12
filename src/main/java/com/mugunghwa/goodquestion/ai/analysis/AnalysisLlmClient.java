package com.mugunghwa.goodquestion.ai.analysis;

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

    /**
     * modelId는 어댑터가 채운다. analysisVersion만으로는 같은 프롬프트를 모델만 바꿔 돌린
     * 경우를 구분할 수 없고, 소급해서 알아낼 방법도 없다.
     */
    public record AnalysisLlmResult(String childIntent, String mainPoint,
                                    java.util.List<DetectedElementDto> detectedElements,
                                    String utteranceValidity, String modelId) {
        public record DetectedElementDto(String type, String evidence) {}
    }
}
