package com.mugunghwa.goodquestion.story.story.dto;

import java.util.List;
import java.util.UUID;

public record StoryDetailResponse(UUID id, String title, String summary, String imageUrl,
                                  String difficulty, List<String> topics,
                                  Short estimatedMinutes, int sceneCount) {
}
