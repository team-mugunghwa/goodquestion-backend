package com.mugunghwa.goodquestion.story.content;

import com.mugunghwa.goodquestion.story.content.dto.StoryDetailResponse;
import com.mugunghwa.goodquestion.story.content.dto.StoryListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    /** topic은 주제 이름이다(예: 다름). 생략하면 전체. */
    @GetMapping
    public StoryListResponse getStories(@RequestParam(required = false) String topic) {
        return storyService.getStories(topic);
    }

    @GetMapping("/{storyId}")
    public StoryDetailResponse getStory(@PathVariable UUID storyId) {
        return storyService.getStory(storyId);
    }
}
