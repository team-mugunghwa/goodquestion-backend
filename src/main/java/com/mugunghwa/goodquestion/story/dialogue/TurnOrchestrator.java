package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.global.vocab.ResponseMode;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.dialogue.dto.AnalysisResponse;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceRequest;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceResponse;
import com.mugunghwa.goodquestion.story.dialogue.engine.ProgressionDecision;
import com.mugunghwa.goodquestion.story.dialogue.engine.ProgressionEngine;
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
import com.mugunghwa.goodquestion.story.session.dto.SceneTransitionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * 발화 처리 파이프라인 오케스트레이터.
 * 분석·진행·표현의 책임 분리 원칙: 각 단계는 하위 컴포넌트가 담당하고 여기서는 순서만 조율한다.
 * 미션 수행 결과(request.missionId 존재)도 동일 파이프라인으로 분석·누적한다 —
 * 미션 check_points가 장면 target_elements와 대응하므로 별도 채점 없이 요소 충족으로 확인한다.
 *
 * <p>순서 자체가 규칙이다. 아래 일곱 단계는 앞 단계의 결과가 뒤 단계의 입력이라 바꿔 끼울 수
 * 없다. 특히 진행 판단(④)은 이번 턴이 누적에 반영된 뒤여야 하고, 진행 상태 응답(⑦)은
 * 장면이 바뀌기 전에 떠 놓아야 끝난 장면 기준으로 나간다.
 */
@Service
@RequiredArgsConstructor
public class TurnOrchestrator {

    private final SessionService sessionService;
    private final MessageService messageService;
    private final UtteranceAnalysisService analysisService;
    private final ProgressionEngine progressionEngine;
    private final MissionPolicy missionPolicy;
    private final MissionConfigReader missionConfigReader;
    private final CharacterResponseService characterResponseService;
    private final SceneClosingHandler sceneClosingHandler;

    @Transactional
    public UtteranceResponse processUtterance(UUID parentId, UUID sessionId, UtteranceRequest request) {
        StorySession session = sessionService.getOwnedSession(parentId, sessionId);
        StoryScene scene = requireOpenDialogueScene(session);

        // ① 아이 발화 저장. 미션 수행 발화면 미션 완료로 표시한다.
        Message childMessage = messageService.appendChild(session, scene, request.text(),
                request.sttRawText(), request.sttConfidence(), request.retryCountOrZero());
        completeMissionIfSubmitted(session, scene, request.missionId());

        // ② 발화 분석. 직전 캐릭터 대사를 함께 넘겨야 무엇에 대한 대답인지 판단할 수 있다.
        UtteranceAnalysis analysis = analysisService.analyze(
                childMessage, scene, messageService.lastCharacterText(sessionId));

        // ③ 누적 상태 갱신. 직전 모드는 여기서 덮이므로 미리 떠 둔다.
        ResponseMode previousMode = session.getLastResponseMode();
        session.applyTurn(elementNames(analysis),
                analysis.getUtteranceValidity().isLowInformation());

        // ④ 진행 판단. 미션 필수 장면은 미션을 끝내기 전에는 목표 달성으로 닫지 않는다.
        ProgressionDecision decision = holdClosingUntilMissionDone(
                progressionEngine.decide(session, scene, previousMode), session, scene);
        session.recordDecision(decision.mode(), decision.guidanceTarget());

        // ⑤ 미션 노출 판단.
        MissionResponse mission = exposeMissionIfNeeded(session, scene, analysis, decision);

        // ⑥ 진행 상태는 장면이 바뀌기 전에 떠 놓는다. 장면을 옮기면 누적 요소가 초기화된다.
        ProgressResponse progress = sessionService.toProgress(session, scene);

        // ⑦ 캐릭터 대사. CLOSING이면 마무리 대사와 장면 이동까지 한 번에 처리된다.
        CharacterMessageResponse characterMessage;
        SceneTransitionResponse sceneTransition = null;
        if (decision.mode() == ResponseMode.CLOSING) {
            SceneClosingHandler.ClosingResult closing =
                    sceneClosingHandler.close(session, scene, decision, analysis);
            characterMessage = closing.message();
            sceneTransition = closing.transition();
        } else {
            characterMessage = appendCharacterReply(session, scene, analysis, decision);
        }

        // safety는 위험 신호 감지가 붙기 전까지 항상 null이다 (SafetyResponse의 TODO).
        return new UtteranceResponse(
                MessageResponse.from(childMessage), AnalysisResponse.from(analysis),
                progress, characterMessage, mission, sceneTransition, null);
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
     */
    private ProgressionDecision holdClosingUntilMissionDone(ProgressionDecision decision,
                                                            StorySession session, StoryScene scene) {
        boolean goalMetClosing = decision.mode() == ResponseMode.CLOSING
                && decision.closingReason() == SceneEndReason.GOAL_MET;
        if (goalMetClosing && scene.hasMission() && !session.isMissionCompleted()) {
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

    private CharacterMessageResponse appendCharacterReply(StorySession session, StoryScene scene,
                                                          UtteranceAnalysis analysis,
                                                          ProgressionDecision decision) {
        CharacterResponseService.CharacterReply reply =
                characterResponseService.reply(session, scene, analysis, decision);
        Message message = messageService.append(session, scene, SpeakerType.CHARACTER,
                reply.text(), null, reply.emotion());
        return CharacterMessageResponse.from(message);
    }

    private List<String> elementNames(UtteranceAnalysis analysis) {
        return analysis.getDetectedElements().stream()
                .map(element -> element.type().name())
                .toList();
    }
}
