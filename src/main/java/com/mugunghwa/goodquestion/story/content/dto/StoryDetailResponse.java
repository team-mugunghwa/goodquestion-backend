package com.mugunghwa.goodquestion.story.content.dto;

import java.util.List;
import java.util.UUID;

public record StoryDetailResponse(UUID id, String title, String summary, String imageUrl,
                                  String difficulty, List<String> topics,
                                  Short estimatedMinutes, int sceneCount) {
}
