package com.mugunghwa.goodquestion.user.parent;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.user.parent.dto.ParentUpdateRequest;
import com.mugunghwa.goodquestion.user.parent.dto.ParentResponse;
import com.mugunghwa.goodquestion.user.parent.dto.PasswordVerifyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/parents")
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;

    @PatchMapping("/me")
    public ParentResponse updateMe(@CurrentParentId UUID parentId,
                                   @Valid @RequestBody ParentUpdateRequest request) {
        return parentService.update(parentId, request);
    }

    @GetMapping("/me")
    public ParentResponse getMe(@CurrentParentId UUID parentId) {
        return parentService.getMe(parentId);
    }

    /** 보호자 확인 게이트. 통과하면 204, 틀리면 401. */
    @PostMapping("/me/verify-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verifyPassword(@CurrentParentId UUID parentId,
                               @Valid @RequestBody PasswordVerifyRequest request) {
        parentService.verifyPassword(parentId, request.password());
    }

}
