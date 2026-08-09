package com.mugunghwa.goodquestion.home;

import com.mugunghwa.goodquestion.home.dto.HomeResponse;
import com.mugunghwa.goodquestion.session.session.SessionStatus;
import com.mugunghwa.goodquestion.session.session.StorySession;
import com.mugunghwa.goodquestion.session.session.StorySessionRepository;
import com.mugunghwa.goodquestion.story.story.Story;
import com.mugunghwa.goodquestion.story.story.StoryRepository;
import com.mugunghwa.goodquestion.story.story.StoryStatus;
import com.mugunghwa.goodquestion.user.child.ChildService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** 메인 화면 조합 서비스 — session + story를 조합만 하고 소유하지 않는다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final ChildService childService;
    private final StorySessionRepository sessionRepository;
    private final StoryRepository storyRepository;

    public HomeResponse getHome(UUID parentId, UUID childId) {
        childService.getOwnedChild(parentId, childId);

        HomeResponse.InProgressSession inProgressSession = sessionRepository
                .findFirstByChildIdAndStatusOrderByLastActivityAtDesc(childId, SessionStatus.IN_PROGRESS)
                .map(this::toInProgressSession)
                .orElse(null);

        // MVP: 추천 로직 미구현 — PUBLISHED 최신순 상위 3개를 그대로 노출
        List<HomeResponse.RecommendedStory> recommendedStories = storyRepository
                .findTop3ByStatusOrderByCreatedAtDesc(StoryStatus.PUBLISHED).stream()
                .map(this::toRecommendedStory)
                .toList();

        return new HomeResponse(inProgressSession, recommendedStories);
    }

    private HomeResponse.InProgressSession toInProgressSession(StorySession session) {
        Story story = session.getStory();
        return new HomeResponse.InProgressSession(
                session.getId(), story.getId(), story.getTitle(), story.getImageUrl(), session.getLastActivityAt());
    }

    private HomeResponse.RecommendedStory toRecommendedStory(Story story) {
        return new HomeResponse.RecommendedStory(
                story.getId(), story.getTitle(), story.getImageUrl(), story.getEstimatedMinutes());
    }
}
