package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.global.vocab.ResponseMode;

/**
 * 캐릭터 대사 생성에 필요한 입력. 트랜잭션 안에서 조립해 밖으로 들고 나간다.
 *
 * <p>아이 이름 치환은 여기서 하지 않는다. 치환 규칙은 세션이 들고 있고(캐릭터-17) 저장 직전
 * 트랜잭션 안에서 적용된다 - 규칙을 두 곳에 두면 첫 대사와 마지막 대사가 갈린다.
 *
 * @param fixedText 값이 있으면 LLM을 부르지 않고 이 대사를 그대로 쓴다. 고정 마지막 대사가
 *                  있는 장면의 CLOSING 턴이 여기 해당한다(캐릭터-12).
 */
public record CharacterPrompt(String childUtterance,
                              String analysisSummary,
                              ResponseMode mode,
                              String characterContext,
                              String remainingWorry,
                              String fixedText) {

    public boolean hasFixedText() {
        return fixedText != null;
    }
}
