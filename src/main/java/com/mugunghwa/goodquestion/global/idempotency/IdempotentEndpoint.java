package com.mugunghwa.goodquestion.global.idempotency;

/** 멱등키가 적용되는 엔드포인트. 중복 실행의 피해가 돈인 곳부터 적용한다. */
public enum IdempotentEndpoint {
    /** 발화 제출 - 중복 처리 시 중복 턴 + LLM 요금 2배. scope는 세션 */
    UTTERANCE,
    /** 아이템 구매 - 중복 처리 시 별가루 이중 차감. scope는 아이 */
    ITEM_PURCHASE,
    /** 자유 대화 발화 - 중복 처리 시 중복 턴 + LLM 요금 2배. scope는 대화 */
    FREE_TALK_MESSAGE
}
