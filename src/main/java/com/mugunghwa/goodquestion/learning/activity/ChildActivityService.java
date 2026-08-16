package com.mugunghwa.goodquestion.learning.activity;

import com.mugunghwa.goodquestion.learning.activity.dto.ChildActivityResponse;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustService;
import com.mugunghwa.goodquestion.story.session.SessionStatus;
import com.mugunghwa.goodquestion.story.session.StorySessionRepository;
import com.mugunghwa.goodquestion.user.child.ChildService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 마이페이지 활동 요약(완주 편수·별가루).
 *
 * <p>user가 아니라 learning에 둔다. 의존 방향이 learning → story → user 단방향이라
 * user에 두면 상위 도메인을 참조하게 되어 ArchitectureTest가 막는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChildActivityService {

    private final ChildService childService;
    private final StardustService stardustService;
    private final StorySessionRepository sessionRepository;

    public ChildActivityResponse getActivity(UUID parentId, UUID childId) {
        childService.getOwnedChild(parentId, childId);
        return new ChildActivityResponse(
                sessionRepository.countDistinctStories(childId, SessionStatus.COMPLETED),
                stardustService.getWallet(parentId, childId).balance());
    }
}