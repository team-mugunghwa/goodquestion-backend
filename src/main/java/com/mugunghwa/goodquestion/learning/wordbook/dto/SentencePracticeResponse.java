package com.mugunghwa.goodquestion.learning.wordbook.dto;

import java.math.BigDecimal;

/**
 * 예문 따라 말하기 결과.
 *
 * <p>화면이 세 갈래를 갈라야 해서 불리언 둘로 준다 — 일치율 미달이면(일치율을 보여 주며
 * "한 번 더?"), 성공인데 보상이 없으면(칭찬만), 성공+보상이면(칭찬 + 별가루 연출).
 *
 * @param matched         일치율이 기준(0.90)을 넘었는지
 * @param similarity      채점된 일치율(0.00~1.00, 소수 둘째 자리). 화면이 "몇 퍼센트"를 보여 줄 수 있게
 * @param targetSentence  채점 기준이 된 예문 — 화면이 어디가 달랐는지 비교해 보여 줄 수 있게
 * @param rewarded        별가루가 지급됐는지
 * @param skipReason      성공인데 지급이 없을 때 그 이유. rewarded=true거나 matched=false면 null
 * @param stardustAmount  이번에 지급된 별가루 수. 지급이 없으면 0 — 화면이 "+n"을 하드코딩하지 않게
 * @param stardustBalance 지급 반영 후 잔액 — 화면이 지갑을 다시 조회하지 않아도 되게
 */
public record SentencePracticeResponse(boolean matched, BigDecimal similarity, String targetSentence,
                                       boolean rewarded, SkipReason skipReason,
                                       int stardustAmount, int stardustBalance) {

    public enum SkipReason {
        /** 이 예문은 이미 보상받았다 — 연습은 언제든 다시 할 수 있다 */
        ALREADY_REWARDED,
        /** 오늘 몫(2건)을 다 받았다 — 이 예문은 내일 다시 성공하면 보상받는다 */
        DAILY_LIMIT
    }
}
