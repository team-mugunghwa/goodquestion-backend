package com.mugunghwa.goodquestion.story.content.dto;

import java.util.List;

/**
 * 이야기 목록 응답.
 * topics는 필터 칩을 그리기 위한 전체 주제 목록이라 topic 필터와 무관하게 항상 전체를 담는다.
 */
public record StoryListResponse(List<StoryCardResponse> stories, List<String> topics) {
}
