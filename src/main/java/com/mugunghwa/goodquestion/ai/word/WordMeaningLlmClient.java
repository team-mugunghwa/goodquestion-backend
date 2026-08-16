package com.mugunghwa.goodquestion.ai.word;

import com.mugunghwa.goodquestion.ai.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/**
 * 단어장 — 아이 수준의 쉬운 뜻과 이야기 속 예문 생성 (단어-02).
 *
 * <p>{@link LlmClient} 공용 포트를 통해 호출한다. 다른 어댑터(CharacterLlmClient 등)와
 * 달리 이 클래스는 예외를 밖으로 던지지 않는다 - 단어 뜻 하나 못 받아온다고 아이의 이야기
 * 진행 자체를 막으면 안 되기 때문이다. 실패(예외, 또는 meaning이 비어 있는 응답)는 전부
 * 대체 문구로 흡수한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WordMeaningLlmClient {

    // 실패(예외/빈 응답)는 realWord=true로 흡수한다 - 생성이 안 됐다고 저장까지
    // 막으면 안 되고, "실제 단어가 아니다"는 모델이 명시적으로 판정했을 때만이다.
    private static final WordMeaningResult FALLBACK =
            new WordMeaningResult("지금은 뜻을 알려줄 수 없어요", null, true);

    private final LlmClient llmClient;
    private final WordMeaningPromptBuilder promptBuilder;

    public WordMeaningResult generate(String word, String sceneContext) {
        try {
            LlmClient.LlmJsonResult result = llmClient.completeJson(
                    promptBuilder.buildSystemPrompt(),
                    promptBuilder.buildUserPrompt(word, sceneContext),
                    "word_meaning",
                    promptBuilder.outputSchema());

            JsonNode json = result.json();
            String meaning = json.path("meaning").asText(null);
            String exampleSentence = json.path("exampleSentence").asText(null);
            // 스키마에 없던 응답(구 모델 캐시 등)이면 true - 판정 불가는 통과다.
            boolean realWord = json.path("isRealWord").asBoolean(true);

            if (!realWord) {
                return new WordMeaningResult(null, null, false);
            }
            if (meaning == null || meaning.isBlank()) {
                log.warn("단어 뜻 생성 실패 - meaning이 비어 있음: word={}", word);
                return FALLBACK;
            }
            return new WordMeaningResult(meaning, exampleSentence, true);
        } catch (Exception e) {
            log.warn("단어 뜻 생성 실패: word={}", word, e);
            return FALLBACK;
        }
    }

    /**
     * @param realWord 실제 쓰이는 우리말 낱말인가. false면 저장을 거절해야 한다 -
     *                 STT 오인식이 만든 존재하지 않는 단어("방비" 부류)가 아이의
     *                 단어장에 영구히 남는 것을 막는 유일한 관문이다
     */
    public record WordMeaningResult(String meaning, String exampleSentence, boolean realWord) {}
}
