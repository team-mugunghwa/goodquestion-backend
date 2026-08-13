package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.global.vocab.ResponseMode;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.dialogue.CharacterResponseService.CharacterReply;
import com.mugunghwa.goodquestion.story.dialogue.dto.AnalysisResponse;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceRequest;
import com.mugunghwa.goodquestion.global.vocab.ReactionKey;
import com.mugunghwa.goodquestion.story.dialogue.engine.ProgressionDecision;
import com.mugunghwa.goodquestion.story.dialogue.engine.ProgressionEngine;
import com.mugunghwa.goodquestion.story.dialogue.engine.ReactionKeyResolver;
import com.mugunghwa.goodquestion.story.mission.MissionConfigReader;
import com.mugunghwa.goodquestion.story.mission.MissionPolicy;
import com.mugunghwa.goodquestion.story.mission.dto.MissionResponse;
import com.mugunghwa.goodquestion.story.session.Message;
import com.mugunghwa.goodquestion.story.session.MessageService;
import com.mugunghwa.goodquestion.story.session.SceneEndReason;
import com.mugunghwa.goodquestion.story.session.SessionService;
import com.mugunghwa.goodquestion.story.session.SpeakerType;
import com.mugunghwa.goodquestion.story.session.StorySession;
import com.mugunghwa.goodquestion.story.session.dto.CharacterMessageResponse;
import com.mugunghwa.goodquestion.story.session.dto.MessageResponse;
import com.mugunghwa.goodquestion.story.session.dto.ProgressResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 턴 처리에서 트랜잭션이 필요한 구간.
 *
 * <p>세 구간 사이에 외부 LLM 호출이 끼어 있어 한 트랜잭션으로 묶을 수 없다. 묶으면 LLM 응답을
 * 기다리는 내내 DB 커넥션을 쥐게 되고, 커넥션 풀은 앱 전체가 공유하므로 대화와 무관한 요청까지
 * 막힌다(docs/트러블슈팅_턴_처리_커넥션_점유.md).
 *
 * <p>{@link TurnOrchestrator}의 private 메서드로 두지 않고 별도 빈으로 뺀 이유는 자기 호출이
 * 프록시를 타지 않아 {@code @Transactional}이 걸리지 않기 때문이다. 판단 로직이 여기 들어와
 * 있는 것은 그 판단들이 붙어 있는 엔티티를 읽어야 하기 때문이고, 트랜잭션 밖에서는 할 수 없다.
 *
 * <p>구간마다 세션을 다시 읽고 상태를 다시 확인한다. 사이에 수 초가 흐르므로 앞 구간에서 본
 * 상태가 그대로라고 가정할 수 없다.
 */
@Service
@RequiredArgsConstructor
public class TurnTransactions {

    private final SessionService sessionService;
    private final MessageService messageService;
    private final UtteranceAnalysisService analysisService;
    private final ProgressionEngine progressionEngine;
    private final ReactionKeyResolver reactionKeyResolver;
    private final MissionPolicy missionPolicy;
    private final MissionConfigReader missionConfigReader;
    private final CharacterResponseService characterResponseService;
    private final SceneClosingHandler sceneClosingHandler;

    /**
     * 분석 LLM을 부르기 전에 필요한 값을 떠 온다.
     *
     * <p>여기서 소유권과 상태를 먼저 보는 이유는 빠른 실패다. 남의 세션이거나 이미 끝난 장면인데
     * LLM부터 부르면 돈과 시간을 쓰고 나서 거절하게 된다.
     */
    @Transactional(readOnly = true)
    public TurnPreparation prepare(UUID parentId, UUID sessionId) {
        StorySession session = sessionService.getOwnedSession(parentId, sessionId);
        StoryScene scene = requireOpenDialogueScene(session);

        return new TurnPreparation(
                SceneAnalysisContext.from(scene), messageService.lastCharacterText(sessionId));
    }

    /**
     * 아이 턴 반영 - 발화와 분석 저장, 누적 갱신, 진행 판단, 미션 노출까지.
     *
     * <p>이 구간이 커밋되면 턴은 되돌릴 수 없다. 캐릭터 대사가 뒤이어 실패해도 아이가 말한
     * 사실과 그에 대한 판단은 남는다 - 이어하기가 그 상태를 그대로 보여준다.
     */
    @Transactional
    public ChildTurnRecord recordChildTurn(UUID parentId, UUID sessionId,
                                           UtteranceRequest request, AnalysisOutcome outcome) {
        StorySession session = sessionService.getOwnedSession(parentId, sessionId);
        StoryScene scene = requireOpenDialogueScene(session);

        // 아이 발화 저장. 미션 수행 발화면 미션 완료로 표시한다.
        Message childMessage = messageService.appendChild(session, scene, request.text(),
                request.sttRawText(), request.sttConfidence(), request.retryCountOrZero());
        completeMissionIfSubmitted(session, scene, request.missionId());
        UtteranceAnalysis analysis = analysisService.save(childMessage, outcome);

        // 누적 상태 갱신. 직전 모드는 여기서 덮이므로 미리 떠 둔다.
        ResponseMode previousMode = session.getLastResponseMode();
        session.applyTurn(outcome.elementNames(), outcome.lowInformation());

        // 진행 판단. 미션 필수 장면은 미션을 끝내기 전에는 목표 달성으로 닫지 않는다.
        ProgressionDecision decision = holdClosingUntilMissionDone(
                progressionEngine.decide(session, scene, previousMode, analysis), session, scene);
        session.recordDecision(decision.mode(), decision.guidanceTarget());

        MissionResponse mission = exposeMissionIfNeeded(session, scene, analysis, decision);

        // 진행 상태는 장면이 바뀌기 전에 떠 놓는다. 장면을 옮기면 누적 요소가 초기화된다.
        ProgressResponse progress = sessionService.toProgress(session, scene);

        // 반응 원칙은 서버가 정해 캐릭터 프롬프트에 넣는다(대화 작동 규칙 3.1). LLM이 고르지 않는다.
        ReactionKey reactionKey = reactionKeyResolver.resolve(
                analysis, decision.mode() == ResponseMode.CLOSING);

        return new ChildTurnRecord(
                MessageResponse.from(childMessage), AnalysisResponse.from(analysis),
                progress, mission, decision,
                characterResponseService.promptFor(scene, outcome, decision, reactionKey, request.text()));
    }

    /**
     * 캐릭터 대사 반영. CLOSING이면 장면 이동이나 후속 활동 전환까지 함께 처리된다.
     *
     * <p>여기서는 장면 상태를 다시 확인하지 않는다. 최대 턴에 닿아 닫히는 턴이 정상 경로이므로
     * 진입 검사를 그대로 적용하면 자기 자신을 거절하게 된다.
     */
    @Transactional
    public TurnClosure recordCharacterReply(UUID parentId, UUID sessionId,
                                            ProgressionDecision decision, CharacterReply reply) {
        StorySession session = sessionService.getOwnedSession(parentId, sessionId);
        StoryScene scene = session.getCurrentScene();

        if (decision.mode() == ResponseMode.CLOSING) {
            return sceneClosingHandler.close(session, scene, decision, reply);
        }

        Message message = messageService.append(session, scene, SpeakerType.CHARACTER,
                session.personalize(reply.text()), null, reply.emotion());
        return new TurnClosure(CharacterMessageResponse.from(message), null);
    }

    /**
     * 이 턴을 받을 수 있는 상태인지 확인한다.
     *
     * <p>최대 턴 검사가 필요한 이유: 장면이 닫히면 다음 장면으로 옮겨 가므로 정상 흐름에서는
     * 걸리지 않는다. 걸린다면 앞선 응답을 받지 못한 클라이언트가 같은 턴을 다시 보낸 것이고,
     * 그대로 처리하면 끝난 장면에 발화가 하나 더 쌓인다.
     */
    private StoryScene requireOpenDialogueScene(StorySession session) {
        if (!session.isInProgress()) {
            throw new BusinessException(ErrorCode.SESSION_NOT_IN_PROGRESS);
        }
        StoryScene scene = session.getCurrentScene();
        if (scene == null || !scene.isDialogue()) {
            throw new BusinessException(ErrorCode.SCENE_NOT_DIALOGUE);
        }
        if (scene.getMaxTurns() != null && session.getCurrentChildTurnCount() >= scene.getMaxTurns()) {
            throw new BusinessException(ErrorCode.MAX_TURNS_EXCEEDED);
        }
        return scene;
    }

    /**
     * 미션 수행 발화 표시. 채점은 하지 않는다 - 미션 check_points가 장면 목표 요소와 대응하므로
     * 요소가 확인되는지로 갈음한다(콘텐츠 문서 공통 처리 규칙).
     */
    private void completeMissionIfSubmitted(StorySession session, StoryScene scene, String missionId) {
        if (missionId == null || !scene.hasMission()) {
            return;
        }
        if (!session.isMissionExposed()) {
            throw new BusinessException(ErrorCode.MISSION_NOT_EXPOSED);
        }
        if (!missionId.equals(missionConfigReader.missionIdOf(scene))) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "이 장면의 미션이 아닙니다.");
        }
        session.completeMission();
    }

    /**
     * 미션 필수 장면의 종료 조건은 "요소 충족 && 미션 완료"다(콘텐츠 문서 출력 조건 3).
     * 미션을 남겨 둔 채 목표만으로 닫으면 미션 화면이 영영 뜨지 않는다.
     *
     * <p>최대 턴 도달로 닫는 것은 막지 않는다. 그것까지 미루면 장면이 끝나지 않는다.
     * 요소를 다 채운 턴은 판정 순서상 최대 턴에 닿아도 GOAL_MET으로 오므로, 마지막 턴의
     * 보류는 최대 턴 종료로 바꾼다 - 그대로 보류하면 장면이 열린 채 턴 수만 최대치가 되고,
     * 이후 발화가 전부 진입 검사의 MAX_TURNS_EXCEEDED에 걸려 미션 수행 발화조차 못 보낸다.
     */
    private ProgressionDecision holdClosingUntilMissionDone(ProgressionDecision decision,
                                                            StorySession session, StoryScene scene) {
        boolean goalMetClosing = decision.mode() == ResponseMode.CLOSING
                && decision.closingReason() == SceneEndReason.GOAL_MET;
        if (goalMetClosing && scene.hasMission() && !session.isMissionCompleted()) {
            if (scene.getMaxTurns() != null
                    && session.getCurrentChildTurnCount() >= scene.getMaxTurns()) {
                return ProgressionDecision.closing(SceneEndReason.MAX_TURNS);
            }
            return ProgressionDecision.normal();
        }
        return decision;
    }

    /** 노출은 장면당 한 번이고, 닫는 턴에는 띄우지 않는다 - 오버레이와 마무리 대사가 겹친다. */
    private MissionResponse exposeMissionIfNeeded(StorySession session, StoryScene scene,
                                                  UtteranceAnalysis analysis,
                                                  ProgressionDecision decision) {
        if (decision.mode() == ResponseMode.CLOSING
                || !scene.hasMission() || session.isMissionExposed()) {
            return null;
        }
        if (!isLastChance(session, scene) && !missionPolicy.shouldExpose(session, scene, analysis)) {
            return null;
        }
        session.exposeMission();
        return missionConfigReader.toResponse(scene);
    }

    /**
     * 요소는 다 채웠는데 미션이 남은 상태. 지금 띄우지 않으면 띄울 자리가 없다 -
     * 미션이 끝나야 닫히는 장면이라 목표 달성으로는 닫히지 않고, 남은 요소가 없어
     * 일반 노출 조건에도 걸리지 않은 채 최대 턴까지 겉돌게 된다.
     */
    private boolean isLastChance(StorySession session, StoryScene scene) {
        return !session.isMissionCompleted()
                && scene.missingElements(session.getAccumulatedElements()).isEmpty();
    }
}
