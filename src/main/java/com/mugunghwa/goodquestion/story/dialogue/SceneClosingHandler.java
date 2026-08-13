package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.story.content.SceneService;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.dialogue.CharacterResponseService.CharacterReply;
import com.mugunghwa.goodquestion.story.dialogue.engine.ProgressionDecision;
import com.mugunghwa.goodquestion.story.session.Message;
import com.mugunghwa.goodquestion.story.session.MessageService;
import com.mugunghwa.goodquestion.story.session.SceneClosedEvent;
import com.mugunghwa.goodquestion.story.session.SceneTransitionTarget;
import com.mugunghwa.goodquestion.story.session.SessionService;
import com.mugunghwa.goodquestion.story.session.SpeakerType;
import com.mugunghwa.goodquestion.story.session.StorySession;
import com.mugunghwa.goodquestion.story.session.dto.CharacterMessageResponse;
import com.mugunghwa.goodquestion.story.session.dto.SceneTransitionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * CLOSING 처리 - 마무리 대사 저장과 다음 장면 이동, 마지막 장면이면 후속 활동 전환.
 *
 * <p>대사 자체는 이미 만들어져 들어온다. 고정 마지막 대사를 쓸지 LLM에 맡길지는
 * {@link CharacterResponseService}가 정하고, 여기서는 저장과 이동만 한다 -
 * LLM 호출이 이 안에 있으면 장면 이동 트랜잭션이 그 시간만큼 커넥션을 쥐게 된다.
 *
 * <p>순서를 지켜야 한다. 마무리 대사는 끝나는 장면에 속하므로 장면을 옮기기 전에 저장하고,
 * 장면 보너스 자격도 옮기기 전에 알린다 - moveToScene이 장면 단위 누적 상태를 지우기 때문이다.
 *
 * <p>호출부가 트랜잭션을 열어 둔 상태여야 한다. 세션과 장면이 붙어 있는 상태로 들어온다.
 */
@Service
@RequiredArgsConstructor
public class SceneClosingHandler {

    private final SessionService sessionService;
    private final SceneService sceneService;
    private final MessageService messageService;
    private final ApplicationEventPublisher eventPublisher;

    public TurnClosure close(StorySession session, StoryScene scene,
                             ProgressionDecision decision, CharacterReply reply) {
        // 미션 필수 장면의 목표는 "요소 충족 && 미션 완료"다. 요소만 채운 채 최대 턴으로
        // 닫힌 장면이 목표 달성(장면 보너스 자격)으로 기록되면 안 된다.
        boolean goalMet = scene.missingElements(session.getAccumulatedElements()).isEmpty()
                && (!scene.hasMission() || session.isMissionCompleted());
        session.closeScene(decision.closingReason(), goalMet);

        Message closingMessage = messageService.append(session, scene, SpeakerType.CHARACTER,
                session.personalize(reply.text()), null, reply.emotion());

        eventPublisher.publishEvent(new SceneClosedEvent(
                session.getId(), scene.getId(), session.isSceneBonusEligible()));

        return sceneService.getNextScene(scene)
                .map(nextScene -> moveOn(session, nextScene, decision, closingMessage))
                .orElseGet(() -> finish(session, decision, closingMessage));
    }

    /**
     * 다음 장면으로 이동한다. 다음이 대화 장면이면 첫 대사도 이때 저장되지만 이 응답에는 담지
     * 않는다 - 화면은 전환을 보고 첫 대사 재생 API(멱등)를 부른다.
     */
    private TurnClosure moveOn(StorySession session, StoryScene nextScene,
                               ProgressionDecision decision, Message closingMessage) {
        sessionService.advanceTo(session, nextScene);

        return new TurnClosure(
                CharacterMessageResponse.from(closingMessage),
                new SceneTransitionResponse(
                        SceneTransitionTarget.SCENE, nextScene.getId(),
                        (int) nextScene.getSceneOrder(), nextScene.getSceneType(),
                        decision.closingReason()));
    }

    /** 마지막 장면이 대화로 끝났다. 후속 활동으로 넘긴다. */
    private TurnClosure finish(StorySession session, ProgressionDecision decision,
                               Message closingMessage) {
        session.toPostActivity();

        return new TurnClosure(
                CharacterMessageResponse.from(closingMessage),
                new SceneTransitionResponse(
                        SceneTransitionTarget.POST_ACTIVITY, null, null, null,
                        decision.closingReason()));
    }
}
