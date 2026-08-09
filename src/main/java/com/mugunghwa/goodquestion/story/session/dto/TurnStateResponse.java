package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.story.session.PlayPhase;

/** 복구·디버그용 턴 상태 조회. */
public record TurnStateResponse(ProgressResponse progress, PlayPhase phase) {
}
