package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceRequest;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceResponse;
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

    @PostMapping("/utterances")
    public UtteranceResponse submit(@CurrentParentId UUID parentId, @PathVariable UUID sessionId,
                                    @Valid @RequestBody UtteranceRequest request) {
        return dialogOrchestrator.processUtterance(parentId, sessionId, request);
    }

    /** 모드·부족 요소·남은 턴 조회 — 복구·디버그용(진행-04, 진행-06). */
    @GetMapping("/turn-state")
    public TurnStateResponse getTurnState(@CurrentParentId UUID parentId,
                                          @PathVariable UUID sessionId) {
        throw new UnsupportedOperationException("미구현: 턴 상태 조회");
    }
}
