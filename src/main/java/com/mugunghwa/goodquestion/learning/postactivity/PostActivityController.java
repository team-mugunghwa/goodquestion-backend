package com.mugunghwa.goodquestion.learning.postactivity;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.learning.postactivity.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions/{sessionId}/post-activity")
@RequiredArgsConstructor
public class PostActivityController {

    private final PostActivityService activityService;

    @GetMapping
    public PostActivityStartResponse start(@CurrentParentId UUID parentId, @PathVariable UUID sessionId) {
        return activityService.start(parentId, sessionId);
    }

    @PostMapping("/order")
    public CardSubmitResponse submitOrder(@CurrentParentId UUID parentId, @PathVariable UUID sessionId,
                                          @Valid @RequestBody CardSubmitRequest request) {
        return activityService.submitOrder(parentId, sessionId, request);
    }

    @PostMapping("/retelling")
    public void submitRetelling(@CurrentParentId UUID parentId, @PathVariable UUID sessionId,
                                @Valid @RequestBody RetellingRequest request) {
        activityService.submitRetelling(parentId, sessionId, request);
    }
}
