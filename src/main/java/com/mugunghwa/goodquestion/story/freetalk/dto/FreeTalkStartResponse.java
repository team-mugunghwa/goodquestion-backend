package com.mugunghwa.goodquestion.story.freetalk.dto;

import java.util.UUID;

/**
 * 자유 대화 시작 응답.
 *
 * @param maxTurns 아이가 말할 수 있는 횟수의 상한. 화면에 남은 턴을 표시하라는 뜻이 아니라,
 *                 클라이언트가 대화 길이를 가늠할 수 있게 내리는 값이다(설계 결정: 표시 없음)
 */
public record FreeTalkStartResponse(UUID freeTalkId, FreeTalkCharacterResponse character,
                                    FreeTalkLineResponse opening, int maxTurns) {
}
