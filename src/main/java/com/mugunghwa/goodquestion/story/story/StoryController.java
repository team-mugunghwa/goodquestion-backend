package com.mugunghwa.goodquestion.story.story;

import com.mugunghwa.goodquestion.story.story.dto.StoryDetailResponse;
import com.mugunghwa.goodquestion.story.story.dto.StoryListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    @GetMapping
    public Page<StoryListResponse> getStories(@RequestParam(required = false) UUID topicId,
                                              @PageableDefault(size = 10) Pageable pageable) {
        return storyService.getStories(topicId, pageable);
    }

    @GetMapping("/{storyId}")
    public StoryDetailResponse getStory(@PathVariable UUID storyId) {
        return storyService.getStory(storyId);
    }
}
