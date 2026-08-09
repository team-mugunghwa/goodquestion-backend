package com.mugunghwa.goodquestion.user.parent;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.user.parent.dto.ParentUpdateRequest;
import com.mugunghwa.goodquestion.user.parent.dto.ParentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
