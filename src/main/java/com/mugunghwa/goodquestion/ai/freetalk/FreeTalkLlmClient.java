package com.mugunghwa.goodquestion.ai.freetalk;

import com.mugunghwa.goodquestion.ai.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * 후속 자유 대화의 캐릭터 대사 LLM 호출.
 *
 * <p>전달: 캐릭터 페르소나(characters.personality), 이야기 제목·줄거리, 지금까지의 대화,
 * 아이의 최신 발화, 단계(첫 인사/이어가기/마무리).
 * 출력: 캐릭터 대사 + 감정(CharacterEmotion 값).
 *
 * <p>학습 대화와 달리 분석 결과도 유도 대상도 넘기지 않는다 - 이 대화에는 판정할 것이
 * 없다. 단계만 서버가 정해서 넘기고 나머지는 연기다.
 */
@Component
@RequiredArgsConstructor
public class FreeTalkLlmClient {

    private final LlmClient llmClient;
    private final FreeTalkPromptBuilder promptBuilder;

    public FreeTalkLlmResult speak(FreeTalkLlmInput input) {
        LlmClient.LlmJsonResult result = llmClient.completeJson(
                promptBuilder.systemPrompt(input),
                promptBuilder.userPrompt(input),
                "free_talk_reply",
                promptBuilder.outputSchema());

        JsonNode json = result.json();
        return new FreeTalkLlmResult(
                json.path("text").asText(null),
                json.path("emotion").asText(null));
    }

    /**
     * @param characterContext 캐릭터 성격·말투. story 도메인을 참조할 수 없어 문자열로 받는다
     * @param history          지금까지 주고받은 말. 시간 순서대로 들어온다
     * @param childUtterance   아이의 최신 발화. 첫 인사(OPENING)에는 없다
     * @param stage            FreeTalkPromptBuilder의 STAGE_* 값
     */
    public record FreeTalkLlmInput(String characterName, String characterContext,
                                   String storyTitle, String storySummary,
                                   List<FreeTalkTurn> history, String childUtterance,
                                   String stage) {}

    /** @param role "CHILD" 또는 "CHARACTER". 도메인 enum을 참조하지 않으려고 문자열로 받는다 */
    public record FreeTalkTurn(String role, String text) {}

    public record FreeTalkLlmResult(String text, String emotion) {}
}
