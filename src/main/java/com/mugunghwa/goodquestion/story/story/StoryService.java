package com.mugunghwa.goodquestion.story.story;

import com.mugunghwa.goodquestion.story.story.dto.StoryDetailResponse;
import com.mugunghwa.goodquestion.story.story.dto.StoryListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryService {

    private final StoryRepository storyRepository;

    public Page<StoryListResponse> getStories(UUID topicId, Pageable pageable) {
        Page<Story> stories = (topicId == null)
                ? storyRepository.findAllByStatus(StoryStatus.PUBLISHED, pageable)
                : storyRepository.findAllByTopicAndStatus(topicId, StoryStatus.PUBLISHED, pageable);
        // TODO: 각 이야기의 토픽 이름 배치 조회 후 매핑 (N+1 방지)
        return stories.map(s -> new StoryListResponse(
                s.getId(), s.getTitle(), s.getImageUrl(), s.getEstimatedMinutes(), null));
    }

    public StoryDetailResponse getStory(UUID storyId) {
        // TODO: PUBLISHED 검증, 장면 수·토픽 이름 조회
        throw new UnsupportedOperationException("TODO");
    }
}
