package com.mugunghwa.goodquestion.infra.llm.prompt;

import org.springframework.stereotype.Component;

/**
 * 캐릭터 응답 프롬프트 조립.
 * 원칙(문서 13~14장): 직접적 학습 질문 금지, 캐릭터 상황·감정 안에서 걱정 표현,
 * 한 번에 하나의 요소만 유도, 직전 유도와 같은 형태 반복 금지, 정답을 캐릭터가 먼저 말하지 않음.
 */
@Component
public class CharacterPromptBuilder {
    // TODO
}
