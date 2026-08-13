package com.mugunghwa.goodquestion.global.vocab;

/**
 * 발화 성격별 반응 원칙 키 (대화 작동 규칙 3.1).
 * 서버 규칙이 분석 결과에서 계산해 캐릭터 LLM에 전달한다 — LLM이 스스로 고르지 않는다.
 */
public enum ReactionKey {
    /** 장난을 실제 사건으로 단정하지 않고 받아친다 */
    PLAYFUL_UTTERANCE,
    /** 아이 질문에 먼저 답한다 */
    QUESTION_FROM_CHILD,
    /** 제안의 도움 되는 점을 인정하고, 중간 턴에는 걱정 하나만 얹는다. SHORT보다 우선 */
    PROPOSAL_FROM_CHILD,
    /** 필요할 때만 짧게 되묻는다 */
    UNCLEAR_UTTERANCE,
    /** 공감 반응 */
    EMPATHY_FROM_CHILD,
    /** 의견·반박·결정 — 무조건 부정하지 않고 걱정 하나 */
    DISAGREEMENT,
    /** 최신 말에 직접 반응. 종료 턴은 의도 매핑을 무시하고 항상 이 키를 쓴다 */
    DIRECT_RESPONSE
}
