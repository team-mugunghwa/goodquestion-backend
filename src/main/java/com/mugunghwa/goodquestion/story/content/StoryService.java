package com.mugunghwa.goodquestion.story.content;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.story.content.dto.StoryDetailResponse;
import com.mugunghwa.goodquestion.story.content.dto.StoryListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryService {

    private final StoryRepository storyRepository;
    private final StorySceneRepository sceneRepository;
    private final StoryTopicRepository storyTopicRepository;

    public Page<StoryListResponse> getStories(UUID topicId, Pageable pageable) {
        Page<Story> stories = (topicId == null)
                ? storyRepository.findAllByStatus(StoryStatus.PUBLISHED, pageable)
                : storyRepository.findAllByTopicAndStatus(topicId, StoryStatus.PUBLISHED, pageable);
        Map<UUID, List<String>> topicNames = findTopicNames(
                stories.getContent().stream().map(Story::getId).toList());

        return stories.map(s -> new StoryListResponse(
                s.getId(), s.getTitle(), s.getImageUrl(), s.getEstimatedMinutes(),
                topicNames.getOrDefault(s.getId(), List.of())));
    }

    public StoryDetailResponse getStory(UUID storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이야기를 찾을 수 없습니다."));
        if (story.getStatus() != StoryStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "이야기를 찾을 수 없습니다.");
        }

        List<String> topics = findTopicNames(List.of(storyId))
                .getOrDefault(storyId, List.of());
        int sceneCount = sceneRepository.countByStoryId(storyId);

        return new StoryDetailResponse(
                story.getId(), story.getTitle(), story.getSummary(), story.getImageUrl(),
                story.getDifficulty(), topics, story.getEstimatedMinutes(), sceneCount);
    }

    /** 이야기별 토픽 이름 배치 조회 — 이야기 수만큼 쿼리가 나가지 않도록 한 번에 가져온다. */
    private Map<UUID, List<String>> findTopicNames(List<UUID> storyIds) {
        if (storyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return storyTopicRepository.findAllByStoryIds(storyIds).stream()
                .collect(Collectors.groupingBy(
                        st -> st.getStory().getId(),
                        Collectors.mapping(st -> st.getTopic().getName(), Collectors.toList())));
    }
}
