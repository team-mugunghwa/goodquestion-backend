package com.mugunghwa.goodquestion.ai.analysis;

import tools.jackson.databind.JsonNode;
import com.mugunghwa.goodquestion.ai.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 발화 분석 LLM 호출 (발화 분석 문서 4·6장 입출력 명세).
 * 입력: sceneContext, goal, previousCharacterMessage, childUtterance, targetElements, elementCriteria
 * 출력: childIntent, detectedElements[{type, evidence}], utteranceValidity (+ mainPoint)
 *
 * <p>여기서는 값 검증을 하지 않는다. enum 매핑과 폴백은 UtteranceAnalysisService가,
 * 근거 검증은 AnalysisPostProcessor가 맡는다 — ai 어댑터는 호출과 변환만 한다.
 */
@Component
@RequiredArgsConstructor
public class AnalysisLlmClient {

    private final LlmClient llmClient;
    private final AnalysisPromptBuilder promptBuilder;

    public AnalysisLlmResult analyze(AnalysisLlmInput input) {
        LlmClient.LlmJsonResult result = llmClient.completeJson(
                promptBuilder.systemPrompt(),
                promptBuilder.userPrompt(input),
                "utterance_analysis",
                promptBuilder.outputSchema());

        JsonNode json = result.json();
        List<AnalysisLlmResult.DetectedElementDto> elements = new ArrayList<>();
        for (JsonNode element : json.path("detectedElements")) {
            elements.add(new AnalysisLlmResult.DetectedElementDto(
                    element.path("type").asText(null), element.path("evidence").asText(null)));
        }
        return new AnalysisLlmResult(
                json.path("childIntent").asText(null),
                json.path("mainPoint").asText(null),
                elements,
                json.path("utteranceValidity").asText(null),
                result.modelId());
    }

    public record AnalysisLlmInput(String sceneContext, String goal, String previousCharacterMessage,
                                   String childUtterance, java.util.List<String> targetElements,
                                   java.util.Map<String, String> elementCriteria) {}

    public record AnalysisLlmResult(String childIntent, String mainPoint,
                                    java.util.List<DetectedElementDto> detectedElements,
                                    String utteranceValidity, String modelId) {
        public record DetectedElementDto(String type, String evidence) {}
    }
}
