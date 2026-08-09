package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceRequest;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceResponse;
import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/utterances")
@RequiredArgsConstructor
public class TurnController {

    private final TurnOrchestrator dialogOrchestrator;

    @PostMapping
    public UtteranceResponse submit(@CurrentParentId UUID parentId, @PathVariable UUID sessionId,
                                    @Valid @RequestBody UtteranceRequest request) {
        return dialogOrchestrator.processUtterance(parentId, sessionId, request);
    }
}
