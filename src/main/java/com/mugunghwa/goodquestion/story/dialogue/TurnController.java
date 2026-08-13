package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.global.idempotency.IdempotencyService;
import com.mugunghwa.goodquestion.global.idempotency.IdempotentEndpoint;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceRequest;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceResponse;
import com.mugunghwa.goodquestion.story.session.SessionService;
import com.mugunghwa.goodquestion.story.session.dto.TurnStateResponse;
import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/{sessionId}")
@RequiredArgsConstructor
public class TurnController {

    private final TurnOrchestrator dialogOrchestrator;
    private final SessionService sessionService;
    private final IdempotencyService idempotencyService;

    /**
     * Idempotency-Key 헤더(선택)를 실으면 타임아웃 후 같은 키의 재전송이 중복 턴이 되지
     * 않는다 - 완료된 요청이면 저장된 응답을 재생한다. 키는 발화마다 새로 만들고
     * 재시도 사이에만 유지한다.
     */
    @PostMapping("/utterances")
    public UtteranceResponse submit(@CurrentParentId UUID parentId, @PathVariable UUID sessionId,
                                    @RequestHeader(value = "Idempotency-Key", required = false)
                                    String idempotencyKey,
                                    @Valid @RequestBody UtteranceRequest request) {
        return idempotencyService.execute(IdempotentEndpoint.UTTERANCE, sessionId, parentId,
                idempotencyKey, UtteranceResponse.class,
                () -> dialogOrchestrator.processUtterance(parentId, sessionId, request));
    }

    /** 모드·부족 요소·남은 턴 조회 — 복구·디버그용(진행-04, 진행-06). */
    @GetMapping("/turn-state")
    public TurnStateResponse getTurnState(@CurrentParentId UUID parentId,
                                          @PathVariable UUID sessionId) {
        return sessionService.getTurnState(parentId, sessionId);
    }
}
