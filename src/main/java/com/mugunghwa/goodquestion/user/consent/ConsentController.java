package com.mugunghwa.goodquestion.user.consent;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.user.consent.dto.ConsentCreateRequest;
import com.mugunghwa.goodquestion.user.consent.dto.ConsentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/children/{childId}/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;
    // TODO: ChildService 주입해 소유권 검증 선행

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsentResponse create(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                                  @Valid @RequestBody ConsentCreateRequest request) {
        return consentService.create(childId, request);
    }

    @PatchMapping("/{consentId}/withdraw")
    public ConsentResponse withdraw(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                                    @PathVariable UUID consentId) {
        return consentService.withdraw(childId, consentId);
    }
}
