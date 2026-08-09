package com.mugunghwa.goodquestion.story.content;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.story.content.dto.StoryCardResponse;
import com.mugunghwa.goodquestion.story.content.dto.StoryDetailResponse;
import com.mugunghwa.goodquestion.story.content.dto.StoryListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final TopicRepository topicRepository;

    /** 공개된 이야기만 노출한다(선택-05). 페이징은 적용하지 않고 전체를 반환한다. */
    public StoryListResponse getStories(String topic) {
        List<Story> stories = StringUtils.hasText(topic)
                ? storyRepository.findAllByTopicNameAndStatus(topic, StoryStatus.PUBLISHED)
                : storyRepository.findAllByStatusOrderByCreatedAtDesc(StoryStatus.PUBLISHED);

        Map<UUID, List<String>> topicNames =
                findTopicNames(stories.stream().map(Story::getId).toList());

        List<StoryCardResponse> cards = stories.stream()
                .map(s -> toCard(s, topicNames.getOrDefault(s.getId(), List.of())))
                .toList();

        List<String> allTopics = topicRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(Topic::getName)
                .toList();

        return new StoryListResponse(cards, allTopics);
    }

    public StoryDetailResponse getStory(UUID storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이야기를 찾을 수 없습니다."));
        if (story.getStatus() != StoryStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "이야기를 찾을 수 없습니다.");
        }
        List<String> topics = findTopicNames(List.of(storyId)).getOrDefault(storyId, List.of());
        int sceneCount = sceneRepository.countByStoryId(storyId);
        return new StoryDetailResponse(toCard(story, topics), sceneCount, null, null);
    }

    private StoryCardResponse toCard(Story story, List<String> topics) {
        return new StoryCardResponse(story.getId(), story.getTitle(), story.getSummary(),
                story.getDifficulty(), story.getEstimatedMinutes(), story.getImageUrl(), topics);
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
