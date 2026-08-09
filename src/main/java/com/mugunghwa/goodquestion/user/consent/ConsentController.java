package com.mugunghwa.goodquestion.user.consent;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.child.ChildService;
import com.mugunghwa.goodquestion.user.consent.dto.ConsentCreateRequest;
import com.mugunghwa.goodquestion.user.consent.dto.ConsentResponse;
import com.mugunghwa.goodquestion.user.consent.dto.ConsentStatusResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/children/{childId}/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;
    private final ChildService childService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsentResponse create(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                                  @Valid @RequestBody ConsentCreateRequest request) {
        Child child = childService.getOwnedChild(parentId, childId);
        return consentService.create(child, request);
    }

    /** 현재 유효 동의 + 이력(계정-10). TODO: ConsentService 조회 메서드 구현. */
    @GetMapping
    public ConsentStatusResponse getStatus(@CurrentParentId UUID parentId, @PathVariable UUID childId) {
        throw new UnsupportedOperationException("미구현: 동의 상태 조회");
    }

    /**
     * 현재 유효한 동의를 철회한다. 이후 신규 세션은 차단된다(계정-13).
     *
     * <p>명세는 consentId 없이 철회하므로 서버가 유효 동의를 찾아야 한다.
     * TODO: ConsentService.withdrawCurrent(childId) 구현 — 아래 구버전은 consentId를 요구한다.
     */
    @PostMapping("/withdraw")
    public ConsentResponse withdraw(@CurrentParentId UUID parentId, @PathVariable UUID childId) {
        throw new UnsupportedOperationException("미구현: 동의 철회(유효 동의 자동 탐색)");
    }
}
