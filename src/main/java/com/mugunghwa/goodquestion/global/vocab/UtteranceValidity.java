package com.mugunghwa.goodquestion.global.vocab;

/** 발화가 진행 판단에 사용할 수 있는 정보량 */
public enum UtteranceValidity {
    VALID, SHORT, UNCLEAR, OFF_TOPIC, PLAYFUL;

    /**
     * 저정보 판정(진행-15). 연속 저정보 턴이 쌓이면 유도 대상이 된다.
     *
     * <p>PLAYFUL은 제외한다 - 장난스러운 발화는 대화에 참여하고 있다는 신호지
     * 정보가 부족하다는 신호가 아니다.
     */
    public boolean isLowInformation() {
        return this == SHORT || this == UNCLEAR || this == OFF_TOPIC;
    }
}
