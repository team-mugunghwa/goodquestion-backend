package com.mugunghwa.goodquestion.story.session;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.story.session.dto.MessageResponse;
import com.mugunghwa.goodquestion.story.session.dto.SceneAdvanceResponse;
import com.mugunghwa.goodquestion.story.session.dto.SceneResponse;
import com.mugunghwa.goodquestion.story.session.dto.SessionResponse;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartRequest;
import com.mugunghwa.goodquestion.story.content.SceneService;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.content.Story;
import com.mugunghwa.goodquestion.story.content.StoryRepository;
import com.mugunghwa.goodquestion.story.content.StoryStatus;
import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.child.ChildService;
import com.mugunghwa.goodquestion.user.consent.ConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public SessionResponse start(UUID parentId, UUID childId, SessionStartRequest request) {
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

        // DIALOGUE 장면은 캐릭터 첫 대사를 재생 시점에 messages로 남긴다.
        // STORY 장면은 내레이션이라 대화 기록에 남기지 않는다.
        MessageResponse openingMessage = null;
        if (firstScene.isDialogue()) {
            Message opening = messageService.append(
                    session, firstScene, SpeakerType.CHARACTER,
                    firstScene.getCharacterOpening(), null, null);
            openingMessage = MessageResponse.from(opening);
        }

        return new SessionResponse(
                session.getId(), session.getStatus(),
                SceneResponse.from(firstScene),
                session.getCurrentChildTurnCount(),
                openingMessage);
    }

    public SessionResponse getSession(UUID parentId, UUID sessionId) {
        StorySession session = getOwnedSession(parentId, sessionId);
        // TODO: SessionResponse 매핑 (openingMessage = null)
        throw new UnsupportedOperationException("TODO");
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
        StoryScene nextScene = sceneService.getNextScene(currentScene).orElse(null);
        if (nextScene == null) {
            session.toPostActivity();
            return new SceneAdvanceResponse(null, null, true);
        }

        session.moveToScene(nextScene);

        // DIALOGUE 장면은 캐릭터 첫 대사를 재생 시점에 messages로 남긴다.
        MessageResponse openingMessage = null;
        if (nextScene.isDialogue()) {
            Message opening = messageService.append(
                    session, nextScene, SpeakerType.CHARACTER,
                    nextScene.getCharacterOpening(), null, null);
            openingMessage = MessageResponse.from(opening);
        }

        return new SceneAdvanceResponse(SceneResponse.from(nextScene), openingMessage, false);
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
}
