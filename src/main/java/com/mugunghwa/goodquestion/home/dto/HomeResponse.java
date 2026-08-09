package com.mugunghwa.goodquestion.home.dto;

import com.mugunghwa.goodquestion.story.content.dto.StoryCardResponse;
import com.mugunghwa.goodquestion.story.session.dto.SessionSummaryResponse;

import java.util.List;

/** 홈 화면 — 이어하기 + 추천 + 섬 위젯을 한 번에 조립한다(홈-01~05). */
public record HomeResponse(
        /** 진행 중인 세션이 없으면 null */
        SessionSummaryResponse inProgressSession,
        List<StoryCardResponse> recommendedStories,
        IslandWidget islandWidget
) {
    /** hasUnacknowledged가 true면 섬 진입 전에 연출 예고 점을 표시한다(보상-08). */
    public record IslandWidget(int stardustBalance, int placedCount, boolean hasUnacknowledged) {}
}
