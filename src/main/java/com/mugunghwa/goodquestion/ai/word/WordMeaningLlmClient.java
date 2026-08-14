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

    private static final WordMeaningResult FALLBACK =
            new WordMeaningResult("지금은 뜻을 알려줄 수 없어요", null);

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

            if (meaning == null || meaning.isBlank()) {
                log.warn("단어 뜻 생성 실패 - meaning이 비어 있음: word={}", word);
                return FALLBACK;
            }
            return new WordMeaningResult(meaning, exampleSentence);
        } catch (Exception e) {
            log.warn("단어 뜻 생성 실패: word={}", word, e);
            return FALLBACK;
        }
    }

    public record WordMeaningResult(String meaning, String exampleSentence) {}
}
