package com.mugunghwa.goodquestion.story.freetalk.dto;

/**
 * 자유 대화 한 턴의 결과.
 *
 * @param turnCount 아이가 지금까지 말한 횟수
 * @param ended     true면 이 대사가 마지막이다. 상한에 닿아 캐릭터가 인사하고 닫혔다
 */
public record FreeTalkTurnResponse(FreeTalkLineResponse characterMessage, int turnCount,
                                   boolean ended) {
}
