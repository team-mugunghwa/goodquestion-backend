package com.mugunghwa.goodquestion.story.content;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.story.content.dto.CompletedStoriesResponse;
import com.mugunghwa.goodquestion.story.content.dto.StoryCardResponse;
import com.mugunghwa.goodquestion.story.content.dto.StoryDetailResponse;
import com.mugunghwa.goodquestion.story.content.dto.StoryListResponse;
import com.mugunghwa.goodquestion.story.content.dto.TopicResponse;
import com.mugunghwa.goodquestion.story.session.SessionStatus;
import com.mugunghwa.goodquestion.story.session.StorySessionRepository;
import com.mugunghwa.goodquestion.user.child.ChildService;
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
    private final TopicService topicService;
    private final StorySessionRepository sessionRepository;
    private final ChildService childService;

    /** 공개된 이야기만 노출한다(선택-05). 페이징은 적용하지 않고 전체를 반환한다. */
    public StoryListResponse getStories(String topic) {
        List<Story> stories = StringUtils.hasText(topic)
                ? storyRepository.findAllByTopicNameAndStatus(topic, StoryStatus.PUBLISHED)
                : storyRepository.findAllByStatusOrderByDisplayOrderAscCreatedAtDesc(StoryStatus.PUBLISHED);

        Map<UUID, List<String>> topicNames =
                findTopicNames(stories.stream().map(Story::getId).toList());

        List<StoryCardResponse> cards = stories.stream()
                .map(s -> toCard(s, topicNames.getOrDefault(s.getId(), List.of())))
                .toList();

        List<String> allTopics = topicService.getTopics().stream()
                .map(TopicResponse::name)
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
        return new StoryDetailResponse(toCard(story, topics), sceneCount,
                story.getChildRole(), story.getIntro());
    }

    /**
     * 이 아이가 완주한 이야기의 id들.
     *
     * <p>이야기 목록 카드에 "끝냈어" 도장을 찍는 데 쓴다. 완주 판정의 근거는 자유 대화의
     * 진입 조건과 <b>같은 COMPLETED 세션</b>이다 - 근거가 갈리면 도장은 찍혔는데 친구는
     * 못 만나는(또는 그 반대의) 화면이 나온다.
     *
     * <p>이야기 목록({@link #getStories})에 얹지 않고 따로 둔 이유 - 저쪽은 <b>누가 보든
     * 같은 답</b>이라 아이를 받지 않는다. 아이별 값을 얹으려면 목록에 {@code childId}를
     * 받아야 하고, 그러면 아이를 안 넘긴 호출에서만 비어 있는 필드가 생긴다. 비어 있는
     * 값과 "완주 안 함"이 같은 모양이면 언젠가 도장이 통째로 안 찍힌다.
     */
    public CompletedStoriesResponse getCompletedStories(UUID parentId, UUID childId) {
        childService.getOwnedChild(parentId, childId);
        return new CompletedStoriesResponse(
                sessionRepository.findDistinctStoryIds(childId, SessionStatus.COMPLETED));
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
