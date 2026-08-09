package com.mugunghwa.goodquestion.home;

import com.mugunghwa.goodquestion.home.dto.HomeResponse;
import com.mugunghwa.goodquestion.learning.reward.island.IslandItemRepository;
import com.mugunghwa.goodquestion.learning.reward.island.IslandRepository;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustTransactionRepository;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWallet;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWalletRepository;
import com.mugunghwa.goodquestion.story.content.Story;
import com.mugunghwa.goodquestion.story.content.StoryRepository;
import com.mugunghwa.goodquestion.story.content.StorySceneRepository;
import com.mugunghwa.goodquestion.story.content.StoryStatus;
import com.mugunghwa.goodquestion.story.content.StoryTopicRepository;
import com.mugunghwa.goodquestion.story.content.dto.StoryCardResponse;
import com.mugunghwa.goodquestion.story.session.SessionStatus;
import com.mugunghwa.goodquestion.story.session.StorySession;
import com.mugunghwa.goodquestion.story.session.StorySessionRepository;
import com.mugunghwa.goodquestion.story.session.dto.SessionSummaryResponse;
import com.mugunghwa.goodquestion.user.child.ChildService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 홈 화면 조립 — story·learning·user를 조회만 해서 한 응답으로 묶는다.
 * 도메인 위에 얹힌 계층이라 상태를 바꾸지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final ChildService childService;
    private final StorySessionRepository sessionRepository;
    private final StoryRepository storyRepository;
    private final StorySceneRepository sceneRepository;
    private final StoryTopicRepository storyTopicRepository;
    private final StardustWalletRepository walletRepository;
    private final StardustTransactionRepository transactionRepository;
    private final IslandRepository islandRepository;
    private final IslandItemRepository islandItemRepository;

    public HomeResponse getHome(UUID parentId, UUID childId) {
        childService.getOwnedChild(parentId, childId);

        SessionSummaryResponse inProgress = sessionRepository
                .findFirstByChildIdAndStatusOrderByLastActivityAtDesc(childId, SessionStatus.IN_PROGRESS)
                .map(this::toSummary)
                .orElse(null);

        // MVP: 추천 로직 미구현 — PUBLISHED 최신순 상위 3개를 그대로 노출한다(홈-03)
        List<Story> recommended = storyRepository.findTop3ByStatusOrderByCreatedAtDesc(StoryStatus.PUBLISHED);
        Map<UUID, List<String>> topicNames = findTopicNames(recommended.stream().map(Story::getId).toList());
        List<StoryCardResponse> cards = recommended.stream()
                .map(s -> new StoryCardResponse(s.getId(), s.getTitle(), s.getSummary(),
                        s.getDifficulty(), s.getEstimatedMinutes(), s.getImageUrl(),
                        topicNames.getOrDefault(s.getId(), List.of())))
                .toList();

        return new HomeResponse(inProgress, cards, toIslandWidget(childId));
    }

    private SessionSummaryResponse toSummary(StorySession session) {
        Story story = session.getStory();
        short sceneOrder = session.getCurrentScene() != null
                ? session.getCurrentScene().getSceneOrder() : 0;
        return new SessionSummaryResponse(
                session.getId(), story.getId(), story.getTitle(), story.getImageUrl(),
                session.getStatus(), sceneOrder, sceneRepository.countByStoryId(story.getId()),
                session.getLastActivityAt());
    }

    /**
     * 섬 위젯(홈-05). 지갑·섬은 아이 생성 시 함께 만들어지지만 그 이전에 만들어진 아이가
     * 있을 수 있어, 없으면 0으로 표시하고 홈 전체를 실패시키지는 않는다.
     */
    private HomeResponse.IslandWidget toIslandWidget(UUID childId) {
        Optional<StardustWallet> wallet = walletRepository.findByChildId(childId);
        int balance = wallet.map(StardustWallet::getBalance).orElse(0);
        boolean hasUnacknowledged = wallet
                .map(w -> !transactionRepository
                        .findAllByWalletIdAndAcknowledgedFalseOrderByCreatedAtAsc(w.getId()).isEmpty())
                .orElse(false);
        int placedCount = islandRepository.findByChildId(childId)
                .map(island -> islandItemRepository.countByIslandId(island.getId()))
                .orElse(0);
        return new HomeResponse.IslandWidget(balance, placedCount, hasUnacknowledged);
    }

    private Map<UUID, List<String>> findTopicNames(List<UUID> storyIds) {
        if (storyIds.isEmpty()) {
            return Map.of();
        }
        return storyTopicRepository.findAllByStoryIds(storyIds).stream()
                .collect(Collectors.groupingBy(
                        st -> st.getStory().getId(),
                        Collectors.mapping(st -> st.getTopic().getName(), Collectors.toList())));
    }
}
