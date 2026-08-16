package com.mugunghwa.goodquestion.learning.wordbook;

/**
 * 따라 말하기 대상 예문 유형 - 이야기/일상/심화(V14의 3종).
 *
 * <p>클라이언트는 유형만 보내고 목표 문장은 서버가 단어에서 꺼낸다. 문장을 클라이언트가
 * 보내게 하면 쉬운 문장으로 바꿔치기해 보상을 딸 수 있다.
 */
public enum ExampleSentenceType {
    STORY, DAILY, ADVANCED;

    /** 단어에서 이 유형의 예문을 꺼낸다. V14 이전에 저장된 단어는 일상/심화가 null일 수 있다. */
    public String from(Wordbook word) {
        return switch (this) {
            case STORY -> word.getExampleSentence();
            case DAILY -> word.getExampleDaily();
            case ADVANCED -> word.getExampleAdvanced();
        };
    }
}
