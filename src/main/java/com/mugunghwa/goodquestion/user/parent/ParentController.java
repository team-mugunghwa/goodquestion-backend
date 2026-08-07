package com.mugunghwa.goodquestion.user.parent;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.user.parent.dto.ParentCreateRequest;
import com.mugunghwa.goodquestion.user.parent.dto.ParentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parents")
@RequiredArgsConstructor
public class ParentController {

    private final ParentService parentService;

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public ParentResponse register(@CurrentParentId UUID parentId,
                                   @Valid @RequestBody ParentCreateRequest request) {
        return parentService.register(parentId, request);
    }

    @GetMapping("/me")
    public ParentResponse getMe(@CurrentParentId UUID parentId) {
        return parentService.getMe(parentId);
    }
}
