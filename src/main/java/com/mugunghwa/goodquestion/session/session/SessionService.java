package com.mugunghwa.goodquestion.session.session;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.session.message.Message;
import com.mugunghwa.goodquestion.session.message.MessageService;
import com.mugunghwa.goodquestion.session.message.SpeakerType;
import com.mugunghwa.goodquestion.session.message.dto.MessageResponse;
import com.mugunghwa.goodquestion.session.session.dto.SceneAdvanceResponse;
import com.mugunghwa.goodquestion.session.session.dto.SceneResponse;
import com.mugunghwa.goodquestion.session.session.dto.SessionResponse;
import com.mugunghwa.goodquestion.session.session.dto.SessionStartRequest;
import com.mugunghwa.goodquestion.story.scene.SceneService;
import com.mugunghwa.goodquestion.story.scene.StoryScene;
import com.mugunghwa.goodquestion.story.story.Story;
import com.mugunghwa.goodquestion.story.story.StoryRepository;
import com.mugunghwa.goodquestion.story.story.StoryStatus;
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
        // TODO: ① IN_PROGRESS + 현재 장면이 STORY인지 검증 (DIALOGUE면 SCENE_NOT_STORY 에러)
        // TODO: ② 다음 장면 조회(SceneService.getNextScene) → session.moveToScene
        // TODO: ③ 다음 장면이 DIALOGUE면 character_opening을 messages에 저장해 응답에 포함
        // TODO: ④ 다음 장면이 없으면(마지막이 STORY인 콘텐츠) session.toPostActivity()
        throw new UnsupportedOperationException("TODO");
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
