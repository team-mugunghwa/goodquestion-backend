package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.story.content.SceneService;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.dialogue.engine.ProgressionDecision;
import com.mugunghwa.goodquestion.story.session.Message;
import com.mugunghwa.goodquestion.story.session.SceneClosedEvent;
import com.mugunghwa.goodquestion.story.session.SceneTransitionTarget;
import com.mugunghwa.goodquestion.story.session.MessageService;
import com.mugunghwa.goodquestion.story.session.SessionService;
import com.mugunghwa.goodquestion.story.session.SpeakerType;
import com.mugunghwa.goodquestion.story.session.StorySession;
import com.mugunghwa.goodquestion.story.session.dto.CharacterMessageResponse;
import com.mugunghwa.goodquestion.story.session.dto.SceneTransitionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * CLOSING 처리.
 * character_closing이 있으면 고정 대사 재생, 없으면 캐릭터 LLM이 마무리 대사 생성
 * (문서 상충 → 서버 정책으로 분기하기로 결정).
 * 이후 다음 장면 이동(opening 저장) 또는 마지막 장면이면 후속 활동 전환.
 *
 * <p>순서를 지켜야 한다. 마무리 대사는 끝나는 장면에 속하므로 장면을 옮기기 전에 저장하고,
 * 장면 보너스 자격도 옮기기 전에 알린다 - moveToScene이 장면 단위 누적 상태를 지우기 때문이다.
 */
@Service
@RequiredArgsConstructor
public class SceneClosingHandler {

    private final SessionService sessionService;
    private final SceneService sceneService;
    private final MessageService messageService;
    private final CharacterResponseService characterResponseService;
    private final ApplicationEventPublisher eventPublisher;

    public ClosingResult close(StorySession session, StoryScene scene,
                               ProgressionDecision decision, UtteranceAnalysis analysis) {
        boolean goalMet = scene.missingElements(session.getAccumulatedElements()).isEmpty();
        session.closeScene(decision.closingReason(), goalMet);

        Message closingMessage = appendClosingMessage(session, scene, decision, analysis);

        eventPublisher.publishEvent(new SceneClosedEvent(
                session.getId(), scene.getId(), session.isSceneBonusEligible()));

        return sceneService.getNextScene(scene)
                .map(nextScene -> moveOn(session, nextScene, decision, closingMessage))
                .orElseGet(() -> finish(session, decision, closingMessage));
    }

    /**
     * 마무리 대사 저장.
     *
     * <p>고정 마지막 대사가 있으면 그것을 쓴다 - 장면을 어떻게 끝냈든 이야기는 같은 자리로
     * 돌아와야 다음 장면이 이어진다. 고정 대사가 없는 장면만 캐릭터 LLM에 맡긴다.
     */
    private Message appendClosingMessage(StorySession session, StoryScene scene,
                                         ProgressionDecision decision, UtteranceAnalysis analysis) {
        if (scene.getCharacterClosing() != null) {
            return messageService.append(session, scene, SpeakerType.CHARACTER,
                    session.personalize(scene.getCharacterClosing()), null, null);
        }
        CharacterResponseService.CharacterReply reply =
                characterResponseService.reply(session, scene, analysis, decision);
        return messageService.append(session, scene, SpeakerType.CHARACTER,
                reply.text(), null, reply.emotion());
    }

    /**
     * 다음 장면으로 이동한다. 다음이 대화 장면이면 첫 대사도 이때 저장되지만 이 응답에는 담지
     * 않는다 - 화면은 전환을 보고 첫 대사 재생 API(멱등)를 부른다.
     */
    private ClosingResult moveOn(StorySession session, StoryScene nextScene,
                                 ProgressionDecision decision, Message closingMessage) {
        sessionService.advanceTo(session, nextScene);

        return new ClosingResult(
                CharacterMessageResponse.from(closingMessage),
                new SceneTransitionResponse(
                        SceneTransitionTarget.SCENE, nextScene.getId(),
                        (int) nextScene.getSceneOrder(), nextScene.getSceneType(),
                        decision.closingReason()));
    }

    /** 마지막 장면이 대화로 끝났다. 후속 활동으로 넘긴다. */
    private ClosingResult finish(StorySession session, ProgressionDecision decision,
                                 Message closingMessage) {
        session.toPostActivity();

        return new ClosingResult(
                CharacterMessageResponse.from(closingMessage),
                new SceneTransitionResponse(
                        SceneTransitionTarget.POST_ACTIVITY, null, null, null,
                        decision.closingReason()));
    }

    public record ClosingResult(CharacterMessageResponse message, SceneTransitionResponse transition) {}
}
