package com.mugunghwa.goodquestion.story.session;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.story.content.dto.SceneContentResponse;
import com.mugunghwa.goodquestion.story.session.dto.*;
import com.mugunghwa.goodquestion.story.content.SceneService;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.content.Story;
import com.mugunghwa.goodquestion.story.content.StoryRepository;
import com.mugunghwa.goodquestion.story.content.StoryStatus;
import com.mugunghwa.goodquestion.story.mission.MissionService;
import com.mugunghwa.goodquestion.story.mission.dto.MissionResponse;
import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.child.ChildService;
import com.mugunghwa.goodquestion.user.consent.ConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionService {

    private final StorySessionRepository sessionRepository;
    private final ChildService childService;
    private final ConsentService consentService;
    private final StoryRepository storyRepository;
    private final SceneService sceneService;
    private final MessageService messageService;
    private final MessageRepository messageRepository;
    private final MissionService missionService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SessionStartResponse start(UUID parentId, UUID childId, SessionStartRequest request) {
        Child child = childService.getOwnedChild(parentId, childId);
        if (!consentService.hasActiveConsent(childId)) {
            throw new BusinessException(ErrorCode.CONSENT_REQUIRED);
        }
        Story story = storyRepository.findById(request.storyId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이야기를 찾을 수 없습니다."));
        if (story.getStatus() != StoryStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "이야기를 찾을 수 없습니다.");
        }

        StoryScene firstScene = sceneService.getFirstScene(story.getId());

        StorySession session = sessionRepository.save(StorySession.builder()
                .child(child)
                .story(story)
                .currentScene(firstScene)
                .build());

        // DIALOGUE 장면은 캐릭터 첫 대사를 재생 시점에 messages로 남긴다(캐릭터-14).
        // STORY 장면은 내레이션이라 대화 기록에 남기지 않는다.
        // 첫 대사는 SessionStartResponse에 담지 않는다 — 클라이언트가 /scenes/current/opening으로 가져간다.
        if (firstScene.isDialogue()) {
            appendOpening(session, firstScene);
        }

        return new SessionStartResponse(
                session.getId(), session.getStatus(),
                SceneContentResponse.from(firstScene),
                session.resolvePhase());
    }

    public SessionResponse getSession(UUID parentId, UUID sessionId) {
        StorySession session = getOwnedSession(parentId, sessionId);
        StoryScene scene = session.getCurrentScene();

        SessionResponse.SceneRef sceneRef = (scene == null) ? null
                : new SessionResponse.SceneRef(scene.getId(), scene.getSceneOrder(), scene.getSceneType());

        return new SessionResponse(
                session.getId(), session.getChild().getId(), session.getStory().getId(),
                session.getStatus(), sceneRef, session.resolvePhase(),
                toProgress(session, scene), session.isSceneGoalMet(), session.getLastActivityAt());
    }

    /** 새로고침·화면 전환 시 현재 장면을 다시 그리기 위한 조회. */
    public CurrentSceneResponse getCurrentScene(UUID parentId, UUID sessionId) {
        StorySession session = getOwnedSession(parentId, sessionId);
        StoryScene scene = session.getCurrentScene();

        return new CurrentSceneResponse(
                scene == null ? null : SceneContentResponse.from(scene),
                session.resolvePhase());
    }

    /**
     * 노출 중인 미션 조회(미션-02). 미노출이면 null이다.
     *
     * <p>컨트롤러가 세션을 직접 꺼내 MissionService에 넘기면 두 호출이 서로 다른 트랜잭션이
     * 되어, 준영속 상태가 된 세션의 currentScene(LAZY)을 건드리는 순간
     * LazyInitializationException이 난다(application.yml의 open-in-view: false).
     * 소유권 검증과 미션 조회를 한 트랜잭션 안에서 끝내야 한다 - resume이 멀쩡한 것과 같은 이유다.
     */
    public MissionResponse currentMission(UUID parentId, UUID sessionId) {
        return missionService.exposedMissionOf(getOwnedSession(parentId, sessionId));
    }

    /**
     * 고정 첫 대사 재생(캐릭터-14). 멱등이다.
     *
     * <p>세션 시작·장면 전환에서 이미 저장했으면 그 메시지를 그대로 돌려준다.
     * 재호출로 같은 대사가 두 번 쌓이면 대화 기록과 turn_order가 어긋난다.
     */
    @Transactional
    public SceneOpeningResponse playOpening(UUID parentId, UUID sessionId) {
        StorySession session = getOwnedSession(parentId, sessionId);
        StoryScene scene = session.getCurrentScene();
        if (scene == null || !scene.isDialogue()) {
            throw new BusinessException(ErrorCode.SCENE_NOT_DIALOGUE);
        }

        return messageRepository
                .findFirstBySessionIdAndSceneIdAndSpeakerTypeOrderByTurnOrderAsc(
                        session.getId(), scene.getId(), SpeakerType.CHARACTER)
                .map(existing -> new SceneOpeningResponse(CharacterMessageResponse.from(existing), true))
                .orElseGet(() -> new SceneOpeningResponse(
                        CharacterMessageResponse.from(appendOpening(session, scene)), false));
    }

    /** 부족 요소는 저장하지 않고 (장면 목표 요소 − 누적 요소)로 매번 계산한다(진행-04). */
    public ProgressResponse toProgress(StorySession session, StoryScene scene) {
        List<ThinkingElement> accumulated = session.getAccumulatedElements().stream()
                .map(ThinkingElement::valueOf).toList();
        List<ThinkingElement> missing = (scene == null)
                ? List.of() : scene.missingElements(session.getAccumulatedElements());

        return new ProgressResponse(
                session.getLastResponseMode(), accumulated, missing,
                session.getCurrentChildTurnCount(),
                (scene == null || scene.getMaxTurns() == null) ? 0 : scene.getMaxTurns(),
                session.getLastGuidanceTarget());
    }

    /** 복구·디버그용 턴 상태 조회(진행-04, 진행-06). */
    public TurnStateResponse getTurnState(UUID parentId, UUID sessionId) {
        StorySession session = getOwnedSession(parentId, sessionId);
        return new TurnStateResponse(
                toProgress(session, session.getCurrentScene()), session.resolvePhase());
    }

    /**
     * STORY 장면(도입·전개) 재생 완료 → 다음 장면 이동.
     * 대화 장면은 이 API가 아니라 CLOSING 파이프라인으로 이동한다.
     */
    @Transactional
    public SceneAdvanceResponse completeStoryScene(UUID parentId, UUID sessionId) {
        StorySession session = getOwnedSession(parentId, sessionId);

        if (!session.isInProgress()) {
            throw new BusinessException(ErrorCode.SESSION_NOT_IN_PROGRESS);
        }
        StoryScene currentScene = session.getCurrentScene();
        if (currentScene.isDialogue()) {
            throw new BusinessException(ErrorCode.SCENE_NOT_STORY);
        }

        // 마지막 장면이 STORY로 끝나는 경우 후속 활동으로 전환한다.
        // 후속 활동 config가 없는 이야기는 건너뛰고 즉시 완료한다(2026-08 확정) -
        // 전환해 두면 카드 시작이 404를 던져 세션이 빠져나갈 수 없다.
        StoryScene nextScene = sceneService.getNextScene(currentScene).orElse(null);
        if (nextScene == null) {
            if (session.getStory().getPostActivityConfig() == null) {
                session.complete();
                eventPublisher.publishEvent(new SessionCompletedEvent(session.getId()));
            } else {
                session.toPostActivity();
            }
            return new SceneAdvanceResponse(session.resolvePhase(), null, null);
        }

        CharacterMessageResponse openingMessage = advanceTo(session, nextScene);

        return new SceneAdvanceResponse(
                session.resolvePhase(), SceneContentResponse.from(nextScene), openingMessage);
    }

    /**
     * 다음 장면으로 이동하고, DIALOGUE면 고정 첫 대사를 저장해 돌려준다(캐릭터-14).
     * STORY 장면은 내레이션이라 대화 기록에 남기지 않으므로 null이다.
     *
     * <p>스토리 재생 완료와 대화 장면 종료가 같은 경로를 타야 한다 - 진입 경로마다 따로
     * 옮기면 한쪽만 첫 대사를 빠뜨리는 식으로 갈라진다.
     */
    @Transactional
    public CharacterMessageResponse advanceTo(StorySession session, StoryScene nextScene) {
        session.moveToScene(nextScene);
        return nextScene.isDialogue()
                ? CharacterMessageResponse.from(appendOpening(session, nextScene)) : null;
    }

    @Transactional
    public void stop(UUID parentId, UUID sessionId) {
        getOwnedSession(parentId, sessionId).stop();
    }

    /** 세션 소유권 검증 공용 — dialog/activity/report에서도 사용 */
    public StorySession getOwnedSession(UUID parentId, UUID sessionId) {
        StorySession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "세션을 찾을 수 없습니다."));
        if (!session.getChild().isOwnedBy(parentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return session;
    }

    /**
     * 고정 첫 대사 저장. 세션 시작·장면 전환·첫 대사 재생이 모두 이 경로를 탄다.
     *
     * <p>진입 경로마다 치환을 따로 하면 같은 대사가 경로에 따라 다르게 저장된다.
     */
    private Message appendOpening(StorySession session, StoryScene scene) {
        String text = session.personalize(scene.getCharacterOpening());
        return messageService.append(session, scene, SpeakerType.CHARACTER, text, null, null);
    }

    /**
     * 이어하기 복원(홈-01~02). 화면을 다시 그리는 데 필요한 것을 한 번에 돌려준다.
     *
     * <p>여러 번 호출로 나누면 그 사이에 턴이 진행돼 장면과 대화 내역이 어긋날 수 있다.
     */
    public SessionResumeResponse resume(UUID parentId, UUID sessionId) {
        StorySession session = getOwnedSession(parentId, sessionId);
        StoryScene scene = session.getCurrentScene();

        CharacterMessageResponse lastCharacterMessage = messageRepository
                .findFirstBySessionIdAndSpeakerTypeOrderByTurnOrderDesc(
                        sessionId, SpeakerType.CHARACTER)
                .map(CharacterMessageResponse::from)
                .orElse(null);

        return new SessionResumeResponse(
                getSession(parentId, sessionId),
                scene == null ? null : SceneContentResponse.from(scene),
                messageService.getMessages(sessionId, null),
                lastCharacterMessage,
                missionService.exposedMissionOf(session));
    }
}