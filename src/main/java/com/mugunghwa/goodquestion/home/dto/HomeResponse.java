package com.mugunghwa.goodquestion.home.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record HomeResponse(
        InProgressSession inProgressSession,     // 없으면 null
        List<RecommendedStory> recommendedStories
) {
    public record InProgressSession(UUID sessionId, UUID storyId, String storyTitle,
                                    String storyImageUrl, OffsetDateTime lastActivityAt) {}

    public record RecommendedStory(UUID storyId, String title, String imageUrl, Short estimatedMinutes) {}
}
