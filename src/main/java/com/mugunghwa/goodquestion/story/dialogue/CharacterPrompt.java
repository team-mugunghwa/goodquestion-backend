package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.global.vocab.ReactionKey;
import com.mugunghwa.goodquestion.global.vocab.ResponseMode;

/**
 * 캐릭터 대사 생성에 필요한 입력. 트랜잭션 안에서 조립해 밖으로 들고 나간다.
 *
 * <p>아이 이름 치환은 여기서 하지 않는다. 치환 규칙은 세션이 들고 있고(캐릭터-17) 저장 직전
 * 트랜잭션 안에서 적용된다 - 규칙을 두 곳에 두면 첫 대사와 마지막 대사가 갈린다.
 *
 * @param characterName    화면 표시용 캐릭터 이름 (프롬프트의 역할 지정)
 * @param reactionKey      발화 성격별 반응 원칙(대화 작동 규칙 3.1). 서버가 계산해 전달한다
 * @param remainingWorry   유도 대상 요소에 대한 캐릭터의 남은 걱정. 없으면 유도 생략(캐릭터-10)
 * @param softCue          true면 약한 유도 — 반응이 중심이고 걱정은 부가(캐릭터-09).
 *                         false에 remainingWorry가 있으면 강한 유도 — 걱정이 핵심 반응(캐릭터-08)
 * @param guidanceStyle    걱정을 어떻게 드러낼지에 대한 캐릭터별 표현 방식(캐릭터-11)
 * @param fixedText        값이 있으면 LLM을 부르지 않고 이 대사를 그대로 쓴다. 고정 마지막 대사가
 *                         있는 장면의 CLOSING 턴이 여기 해당한다(캐릭터-12)
 */
public record CharacterPrompt(String childUtterance,
                              String analysisSummary,
                              ResponseMode mode,
                              ReactionKey reactionKey,
                              String characterName,
                              String characterContext,
                              String remainingWorry,
                              boolean softCue,
                              String guidanceStyle,
                              String fixedText) {

    public boolean hasFixedText() {
        return fixedText != null;
    }
}
