package com.mugunghwa.goodquestion.story.freetalk.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 자유 대화 상대 후보 1명.
 *
 * @param characterKey  표정 이미지 파일명의 키. 클라이언트가 {characterKey}_{expression}.png로
 *                      조립한다 - 서버는 이 규칙의 이름만 내리고 경로를 만들지 않는다
 * @param thumbnailUrl  서버가 가진 캐릭터 이미지. 지금은 캐릭터 이미지가 클라이언트 자산이라
 *                      항상 null이다. characters에 이미지 컬럼이 생기면 그때 채운다
 * @param lastTalkedAt  이 인물과 마지막으로 이야기한 시각. 한 번도 없으면 null
 */
public record FreeTalkCharacterResponse(UUID characterId, String name, String characterKey,
                                        String thumbnailUrl, OffsetDateTime lastTalkedAt) {
}
