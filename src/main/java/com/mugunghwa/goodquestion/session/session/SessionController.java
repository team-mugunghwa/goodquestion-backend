package com.mugunghwa.goodquestion.session.session;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.session.message.MessageService;
import com.mugunghwa.goodquestion.session.message.dto.MessageResponse;
import com.mugunghwa.goodquestion.session.session.dto.SceneAdvanceResponse;
import com.mugunghwa.goodquestion.session.session.dto.SessionResponse;
import com.mugunghwa.goodquestion.session.session.dto.SessionStartRequest;
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

    @PostMapping("/api/v1/children/{childId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse start(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                                 @Valid @RequestBody SessionStartRequest request) {
        return sessionService.start(parentId, childId, request);
    }

    @GetMapping("/api/v1/sessions/{sessionId}")
    public SessionResponse getSession(@CurrentParentId UUID parentId, @PathVariable UUID sessionId) {
        return sessionService.getSession(parentId, sessionId);
    }

    @GetMapping("/api/v1/sessions/{sessionId}/messages")
    public List<MessageResponse> getMessages(@CurrentParentId UUID parentId,
                                             @PathVariable UUID sessionId,
                                             @RequestParam(required = false) UUID sceneId) {
        sessionService.getOwnedSession(parentId, sessionId);
        return messageService.getMessages(sessionId, sceneId);
    }

    /** STORY 장면(도입·전개) 재생 완료 통지 → 다음 장면 이동 */
    @PostMapping("/api/v1/sessions/{sessionId}/scene-completions")
    public SceneAdvanceResponse completeStoryScene(@CurrentParentId UUID parentId,
                                                   @PathVariable UUID sessionId) {
        return sessionService.completeStoryScene(parentId, sessionId);
    }

    @PatchMapping("/api/v1/sessions/{sessionId}/stop")
    public void stop(@CurrentParentId UUID parentId, @PathVariable UUID sessionId) {
        sessionService.stop(parentId, sessionId);
    }
}
