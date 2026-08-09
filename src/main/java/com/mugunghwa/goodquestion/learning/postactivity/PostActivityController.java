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

    /** 카드 셔플 순서를 서버가 고정한다 — 재호출해도 같은 순서를 돌려준다(활동-02). */
    @PostMapping("/start")
    public PostActivityStartResponse start(@CurrentParentId UUID parentId, @PathVariable UUID sessionId) {
        return activityService.start(parentId, sessionId);
    }

    /** 새로고침 복구용 상태 조회(활동-08). TODO: PostActivityService 조회 메서드 구현. */
    @GetMapping
    public PostActivityStatusResponse getStatus(@CurrentParentId UUID parentId,
                                                @PathVariable UUID sessionId) {
        throw new UnsupportedOperationException("미구현: 후속 활동 상태 조회");
    }

    @PostMapping("/order")
    public CardSubmitResponse submitOrder(@CurrentParentId UUID parentId, @PathVariable UUID sessionId,
                                          @Valid @RequestBody CardSubmitRequest request) {
        return activityService.submitOrder(parentId, sessionId, request);
    }

    /** 세션 완료 + 별가루 지급을 함께 수행하고 지급 내역을 돌려준다(활동-09~10). */
    @PostMapping("/retelling")
    public RetellingResponse submitRetelling(@CurrentParentId UUID parentId, @PathVariable UUID sessionId,
                                             @Valid @RequestBody RetellingRequest request) {
        return activityService.submitRetelling(parentId, sessionId, request);
    }
}
