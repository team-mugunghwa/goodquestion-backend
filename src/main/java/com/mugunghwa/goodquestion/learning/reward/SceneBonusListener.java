package com.mugunghwa.goodquestion.learning.reward;

import com.mugunghwa.goodquestion.learning.reward.stardust.StardustService;
import com.mugunghwa.goodquestion.story.content.StorySceneRepository;
import com.mugunghwa.goodquestion.story.session.SceneClosedEvent;
import com.mugunghwa.goodquestion.story.session.StorySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 유도 없이 목표를 통과한 장면에 별가루 보너스를 지급한다(보상-04).
 *
 * <p>중복 지급과 세션 상한은 {@link StardustService#awardSceneBonus}가 막으므로
 * 여기서는 자격 여부만 보고 넘긴다.
 */
@Component
@RequiredArgsConstructor
public class SceneBonusListener {

    private final StardustService stardustService;
    private final StorySessionRepository sessionRepository;
    private final StorySceneRepository sceneRepository;

    @EventListener
    public void award(SceneClosedEvent event) {
        if (!event.bonusEligible()) {
            return;
        }
        stardustService.awardSceneBonus(
                sessionRepository.getReferenceById(event.sessionId()),
                sceneRepository.getReferenceById(event.sceneId()));
    }
}
