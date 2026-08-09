package com.mugunghwa.goodquestion.learning.postactivity.dto;

import java.util.List;

/**
 * 후속 활동 시작 — 카드를 무작위 순서로 준다(활동-02).
 * 정답 순서는 응답에 담지 않는다. 판정은 서버만 한다(활동-04).
 */
public record PostActivityStartResponse(List<Card> cards, short attemptCount) {

    /** 명세 3-14 후속 활동 카드 */
    public record Card(String cardId, String text) {}
}
