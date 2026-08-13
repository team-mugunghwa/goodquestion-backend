package com.mugunghwa.goodquestion.ai.character;

import tools.jackson.databind.JsonNode;
import com.mugunghwa.goodquestion.ai.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 캐릭터 응답 LLM 호출 (NORMAL/GUIDED).
 * 전달: 아이 최신 발화, 분석 결과, 진행 모드, 반응 원칙(reactionKey), 캐릭터 상황,
 *      (유도 시) 남은 걱정과 표현 방식(guidanceStyle).
 * 출력: 캐릭터 대사 + 감정(CharacterEmotion 값).
 *
 * <p>모드·반응 원칙·유도 대상은 서버가 정해서 들어온다. 여기서는 연기만 시킨다(정책-02).
 */
@Component
@RequiredArgsConstructor
public class CharacterLlmClient {

    private final LlmClient llmClient;
    private final CharacterPromptBuilder promptBuilder;

    public CharacterLlmResult reply(CharacterLlmInput input) {
        LlmClient.LlmJsonResult result = llmClient.completeJson(
                promptBuilder.systemPrompt(input),
                promptBuilder.userPrompt(input),
                "character_reply",
                promptBuilder.outputSchema());

        JsonNode json = result.json();
        return new CharacterLlmResult(
                json.path("text").asText(null),
                json.path("emotion").asText(null));
    }

    /**
     * @param reactionKey ReactionKey.name(). ai 패키지는 도메인을 참조하지 않으므로 문자열로 받는다
     * @param softCue     true면 약한 유도(반응 중심), false에 remainingWorry가 있으면 강한 유도
     */
    public record CharacterLlmInput(String childUtterance, String analysisSummary, String mode,
                                    String reactionKey, String characterName, String characterContext,
                                    String remainingWorry, boolean softCue, String guidanceStyle) {}

    public record CharacterLlmResult(String text, String emotion) {}
}
