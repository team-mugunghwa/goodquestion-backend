package com.mugunghwa.goodquestion.story.freetalk;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 인물별 마지막 자유 대화 시각. 인물 고르기 화면의 "언제 이야기했는지"에 쓴다. */
public record LastTalk(UUID characterId, OffsetDateTime lastTalkedAt) {
}
