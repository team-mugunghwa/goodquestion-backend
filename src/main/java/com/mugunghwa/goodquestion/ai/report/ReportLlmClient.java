package com.mugunghwa.goodquestion.ai.report;

import com.mugunghwa.goodquestion.ai.llm.LlmClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 보호자 리포트 생성 LLM 호출 (리포트 요건 3~7절).
 *
 * <p>여기서는 값 검증을 하지 않는다. enum 매핑과 빈 값 처리는 ReportService가 맡는다 —
 * ai 어댑터는 호출과 변환만 한다.
 */
@Component
public class ReportLlmClient {

    private final LlmClient llmClient;
    private final ReportPromptBuilder promptBuilder;
    private final Duration timeout;

    public ReportLlmClient(LlmClient llmClient, ReportPromptBuilder promptBuilder,
                           @Value("${external.report.timeout-ms:30000}") long timeoutMs) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    public ReportLlmResult generate(ReportLlmInput input) {
        LlmClient.LlmJsonResult result = llmClient.completeJson(
                promptBuilder.systemPrompt(),
                promptBuilder.userPrompt(input),
                "guardian_report",
                promptBuilder.outputSchema(),
                // 전용 타임아웃을 반드시 넘긴다. 4-인자 버전을 부르면 공용 10초에 걸리는데,
                // 리포트는 세션 전체 대화를 넣고 긴 JSON을 받아 공용 값으로는 끊긴다 —
                // LlmClient 인터페이스 주석이 경고하는 바로 그 형태였다(08-15 감사 ⑤).
                timeout);

        JsonNode json = result.json();
        return new ReportLlmResult(
                json.path("summary").asText(null),
                items(json.path("strengths")),
                items(json.path("nextFocus")),
                vocabulary(json.path("vocabulary")),
                competencies(json.path("competencies")),
                representativeUtterance(json.path("representativeUtterance")),
                homeGuide(json.path("homeGuide")),
                result.modelId());
    }

    private List<ItemDto> items(JsonNode array) {
        List<ItemDto> items = new ArrayList<>();
        for (JsonNode node : array) {
            items.add(new ItemDto(node.path("element").asText(null),
                    node.path("comment").asText(null)));
        }
        return items;
    }

    private VocabularyDto vocabulary(JsonNode node) {
        return new VocabularyDto(
                texts(node.path("mainWords")),
                texts(node.path("askedWords")),
                texts(node.path("repeatedExpressions")),
                node.path("feedback").asText(null));
    }

    private List<CompetencyDto> competencies(JsonNode array) {
        List<CompetencyDto> competencies = new ArrayList<>();
        for (JsonNode node : array) {
            competencies.add(new CompetencyDto(
                    node.path("name").asText(null),
                    node.path("finding").asText(null),
                    node.path("evidenceUtterance").asText(null),
                    node.path("strength").asText(null),
                    node.path("nextFocus").asText(null)));
        }
        return competencies;
    }

    private RepresentativeUtteranceDto representativeUtterance(JsonNode node) {
        return new RepresentativeUtteranceDto(
                node.path("text").asText(null),
                node.path("reason").asText(null));
    }

    private HomeGuideDto homeGuide(JsonNode node) {
        return new HomeGuideDto(
                texts(node.path("storyQuestions")),
                texts(node.path("dailyLifeQuestions")));
    }

    private List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (JsonNode node : array) {
            values.add(node.asText(null));
        }
        return values;
    }

    /**
     * 세션 한 건의 대화 기록.
     *
     * <p>인식이 미덥지 않았던 발화는 호출부에서 미리 걸러 넣는다 — 저장된 원문이 아이가
     * 실제로 한 말과 다를 수 있는데, 리포트는 "아이가 이렇게 말했다"고 보여주는 자리다.
     */
    public record ReportLlmInput(String storyTitle, String childName, int childAge,
                                 List<TurnDigest> turns) {}

    /** 캐릭터 대사와 아이 발화 한 쌍. 내레이션 장면은 characterName이 null이다. */
    public record TurnDigest(short sceneOrder, String characterName, String characterText,
                             String childText, List<String> detectedElements) {}

    public record ReportLlmResult(String summary,
                                  List<ItemDto> strengths,
                                  List<ItemDto> nextFocus,
                                  VocabularyDto vocabulary,
                                  List<CompetencyDto> competencies,
                                  RepresentativeUtteranceDto representativeUtterance,
                                  HomeGuideDto homeGuide,
                                  String modelId) {}

    public record ItemDto(String element, String comment) {}

    public record VocabularyDto(List<String> mainWords, List<String> askedWords,
                                List<String> repeatedExpressions, String feedback) {}

    public record CompetencyDto(String name, String finding, String evidenceUtterance,
                                String strength, String nextFocus) {}

    public record RepresentativeUtteranceDto(String text, String reason) {}

    public record HomeGuideDto(List<String> storyQuestions, List<String> dailyLifeQuestions) {}
}