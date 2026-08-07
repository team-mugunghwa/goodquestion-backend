package com.mugunghwa.goodquestion.home;

import com.mugunghwa.goodquestion.home.dto.HomeResponse;
import com.mugunghwa.goodquestion.session.session.StorySessionRepository;
import com.mugunghwa.goodquestion.story.story.StoryRepository;
import com.mugunghwa.goodquestion.user.child.ChildService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // TODO: ① IN_PROGRESS 세션 중 last_activity_at 최신 1건 조회
        // TODO: ② 추천 이야기 — MVP는 PUBLISHED 최신순 2~3개 (추천 로직 미구현 요건 반영)
        throw new UnsupportedOperationException("TODO");
    }
}
