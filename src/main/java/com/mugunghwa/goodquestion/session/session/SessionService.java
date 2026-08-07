package com.mugunghwa.goodquestion.session.session;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.session.session.dto.SceneAdvanceResponse;
import com.mugunghwa.goodquestion.session.session.dto.SessionResponse;
import com.mugunghwa.goodquestion.session.session.dto.SessionStartRequest;
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
    // TODO: SceneService, MessageService 주입

    @Transactional
    public SessionResponse start(UUID parentId, UUID childId, SessionStartRequest request) {
        childService.getOwnedChild(parentId, childId);
        if (!consentService.hasActiveConsent(childId)) {
            throw new BusinessException(ErrorCode.CONSENT_REQUIRED);
        }
        // TODO: ① PUBLISHED 이야기 검증 ② 첫 장면 조회 ③ 세션 생성
        // TODO: ④ character_opening을 messages에 CHARACTER 발화로 저장 후 응답에 포함
        throw new UnsupportedOperationException("TODO");
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
