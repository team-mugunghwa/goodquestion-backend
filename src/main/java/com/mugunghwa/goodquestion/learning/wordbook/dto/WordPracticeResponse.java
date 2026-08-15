package com.mugunghwa.goodquestion.learning.wordbook.dto;

/**
 * 단어 말하기 연습 결과.
 *
 * <p>화면이 세 갈래를 갈라야 해서 불리언 둘로 준다 — 문장에 단어가 없으면(캐릭터가 "한 번 더?"),
 * 성공인데 보상이 없으면(칭찬만), 성공+보상이면(칭찬 + 별가루 연출).
 *
 * @param matched  문장에 단어가 들어 있었는지
 * @param rewarded 별가루가 지급됐는지
 * @param skipReason 성공인데 지급이 없을 때 그 이유. rewarded=true거나 matched=false면 null
 * @param stardustBalance 지급 반영 후 잔액 — 화면이 지갑을 다시 조회하지 않아도 되게
 */
public record WordPracticeResponse(boolean matched, boolean rewarded,
                                   SkipReason skipReason, int stardustBalance) {

    public enum SkipReason {
        /** 이 단어는 이미 보상받았다 — 연습은 언제든 다시 할 수 있다 */
        ALREADY_REWARDED,
        /** 오늘 몫(3개)을 다 받았다 — 이 단어는 내일 다시 성공하면 보상받는다 */
        DAILY_LIMIT,
        /** 문장에 단어가 없었다 */
        WORD_NOT_IN_SENTENCE
    }
}
