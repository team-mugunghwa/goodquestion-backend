package com.mugunghwa.goodquestion.story.freetalk;

import com.mugunghwa.goodquestion.ai.freetalk.FreeTalkLlmClient;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkCharacterResponse;

import java.util.UUID;

/**
 * 트랜잭션 안에서 조립해 밖으로 들고 나가는 값. 엔티티를 들고 나가지 않는다 -
 * open-in-view가 꺼져 있어 트랜잭션이 끝나면 LAZY 필드를 못 읽는다.
 *
 * @param freeTalkId 아직 만들지 않은 대화(시작 준비)면 null
 * @param character  시작 응답에만 필요하다. 턴 처리에서는 null
 * @param turnCount  이 발화가 몇 번째 턴이 될지. 준비 시점의 값에 1을 더한 것이라
 *                   아직 확정이 아니다 - 저장은 조건부 갱신이 이기는 요청만 통과시킨다
 * @param lastTurn   true면 이 대사로 대화가 닫힌다(턴 상한 도달)
 */
public record FreeTalkContext(UUID freeTalkId,
                              FreeTalkCharacterResponse character,
                              FreeTalkLlmClient.FreeTalkLlmInput llmInput,
                              int turnCount,
                              boolean lastTurn) {

    /** TTS 보이스 매핑 키. 캐릭터 표시 이름이 그대로 쓰인다. */
    public String characterName() {
        return llmInput.characterName();
    }

    /** 조건부 갱신이 대조할 "읽었을 때의 턴 수". */
    public short expectedTurnCount() {
        return (short) (turnCount - 1);
    }
}
