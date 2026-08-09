package com.mugunghwa.goodquestion.story.content.dto;

import java.util.List;
import java.util.UUID;

public record StoryListResponse(UUID id, String title, String imageUrl,
                                Short estimatedMinutes, List<String> topics) {
}
