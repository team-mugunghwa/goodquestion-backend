package com.mugunghwa.goodquestion.story.session;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.story.session.dto.CompletedStoriesResponse;
import com.mugunghwa.goodquestion.story.session.dto.MessageResponse;
import com.mugunghwa.goodquestion.story.session.dto.SceneAdvanceResponse;
import com.mugunghwa.goodquestion.story.session.dto.SessionResponse;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartResponse;
import com.mugunghwa.goodquestion.story.session.dto.SessionResumeResponse;
import com.mugunghwa.goodquestion.story.session.dto.SessionStartRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final MessageService messageService;

    @PostMapping("/api/children/{childId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionStartResponse start(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                                      @Valid @RequestBody SessionStartRequest request) {
        return sessionService.start(parentId, childId, request);
    }

    /**
     * 이 아이가 완주한 이야기의 id들. 이야기 목록 화면이 카드에 "끝냈어" 도장을 찍는 데 쓴다.
     *
     * <p><b>이야기 모양의 경로인데 세션 컨트롤러에 있다.</b> 완주는 이야기의 성질이 아니라
     * (아이, 이야기)의 런타임 상태이고, 그 상태를 아는 것은 세션뿐이다. 콘텐츠 패키지는
     * 런타임 상태를 알지 못한다는 규칙이 있다(데이터-02,
     * {@code ArchitectureTest.content_must_not_depend_on_runtime}).
     *
     * <p>한 번도 완주하지 않았으면 빈 배열이다 - 404가 아니다. 아이가 아직 아무것도 안 한
     * 것은 정상이고, 목록 화면은 그 응답으로 도장만 안 찍으면 된다.
     */
    @GetMapping("/api/children/{childId}/stories/completed")
    public CompletedStoriesResponse getCompletedStories(@CurrentParentId UUID parentId,
                                                        @PathVariable UUID childId) {
        return sessionService.getCompletedStories(parentId, childId);
    }

    @GetMapping("/api/sessions/{sessionId}")
    public SessionResponse getSession(@CurrentParentId UUID parentId, @PathVariable UUID sessionId) {
        return sessionService.getSession(parentId, sessionId);
    }

    /** 이어하기 — 장면·대화 내역·마지막 대사·노출 미션을 한 번에 복원한다(홈-01~02). */
    @GetMapping("/api/sessions/{sessionId}/resume")
    public SessionResumeResponse resume(@CurrentParentId UUID parentId, @PathVariable UUID sessionId) {
        return sessionService.resume(parentId, sessionId);
    }

    @GetMapping("/api/sessions/{sessionId}/messages")
    public List<MessageResponse> getMessages(@CurrentParentId UUID parentId,
                                             @PathVariable UUID sessionId,
                                             @RequestParam(required = false) UUID sceneId) {
        sessionService.getOwnedSession(parentId, sessionId);
        return messageService.getMessages(sessionId, sceneId);
    }

    /**
     * STORY 장면(도입, 전개) 재생 완료 통지 -> 다음 장면 이동.
     *
     * <p>{@code sceneId}는 방금 다 본 장면이다. 이 값으로 중복 호출을 가려낸다 -
     * 재전송이나 두 번 누름으로 같은 요청이 두 번 오면, 두 번째는 이미 다음 장면에
     * 와 있으므로 전진하지 않고 현재 상태를 그대로 돌려준다. 없으면 장면 하나를
     * 통째로 건너뛰어 아이가 그 대목을 못 본다.
     *
     * <p>선택 파라미터다 - 구버전 클라이언트는 지금까지처럼 무조건 전진한다.
     * 클라이언트가 모두 올라오면 필수로 바꾼다.
     */
    @PostMapping("/api/sessions/{sessionId}/scenes/current/story-complete")
    public SceneAdvanceResponse completeStoryScene(@CurrentParentId UUID parentId,
                                                   @PathVariable UUID sessionId,
                                                   @RequestParam(required = false) UUID sceneId) {
        return sessionService.completeStoryScene(parentId, sessionId, sceneId);
    }

    @PostMapping("/api/sessions/{sessionId}/stop")
    public void stop(@CurrentParentId UUID parentId, @PathVariable UUID sessionId) {
        sessionService.stop(parentId, sessionId);
    }
}
