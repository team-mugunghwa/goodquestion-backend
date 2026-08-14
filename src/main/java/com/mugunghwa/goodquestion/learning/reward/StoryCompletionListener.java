package com.mugunghwa.goodquestion.learning.reward;

import com.mugunghwa.goodquestion.learning.reward.stardust.StardustService;
import com.mugunghwa.goodquestion.story.session.SessionCompletedEvent;
import com.mugunghwa.goodquestion.story.session.StorySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 후속 활동 없이 완료된 세션의 완주 별가루 지급 (SessionCompletedEvent 참고).
 * 동기 리스너라 완료 처리와 같은 트랜잭션이다 - 실패하면 완료도 함께 롤백된다.
 * 지급 자체는 멱등(세션당 1회 유니크)이다.
 */
@Component
@RequiredArgsConstructor
public class StoryCompletionListener {

    private final StardustService stardustService;
    private final StorySessionRepository sessionRepository;

    @EventListener
    public void award(SessionCompletedEvent event) {
        stardustService.awardStoryCompleted(sessionRepository.getReferenceById(event.sessionId()));
    }
}
