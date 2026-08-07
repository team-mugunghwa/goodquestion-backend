package com.mugunghwa.goodquestion.learning.activity;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.learning.activity.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/post-activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    public ActivityStartResponse start(@CurrentParentId UUID parentId, @PathVariable UUID sessionId) {
        return activityService.start(parentId, sessionId);
    }

    @PostMapping("/card-orders")
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
