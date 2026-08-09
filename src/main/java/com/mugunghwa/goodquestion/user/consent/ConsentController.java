package com.mugunghwa.goodquestion.user.consent;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.child.ChildService;
import com.mugunghwa.goodquestion.user.consent.dto.ConsentCreateRequest;
import com.mugunghwa.goodquestion.user.consent.dto.ConsentResponse;
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

    @PatchMapping("/{consentId}/withdraw")
    public ConsentResponse withdraw(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                                    @PathVariable UUID consentId) {
        childService.getOwnedChild(parentId, childId);
        return consentService.withdraw(childId, consentId);
    }
}
