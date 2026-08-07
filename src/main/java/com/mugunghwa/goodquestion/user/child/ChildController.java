package com.mugunghwa.goodquestion.user.child;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.user.child.dto.ChildCreateRequest;
import com.mugunghwa.goodquestion.user.child.dto.ChildResponse;
import com.mugunghwa.goodquestion.user.child.dto.ChildUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/children")
@RequiredArgsConstructor
public class ChildController {

    private final ChildService childService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChildResponse create(@CurrentParentId UUID parentId,
                                @Valid @RequestBody ChildCreateRequest request) {
        return childService.create(parentId, request);
    }

    @GetMapping
    public List<ChildResponse> getMyChildren(@CurrentParentId UUID parentId) {
        return childService.getMyChildren(parentId);
    }

    @GetMapping("/{childId}")
    public ChildResponse getChild(@CurrentParentId UUID parentId, @PathVariable UUID childId) {
        return childService.getChild(parentId, childId);
    }

    @PatchMapping("/{childId}")
    public ChildResponse update(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                                @Valid @RequestBody ChildUpdateRequest request) {
        return childService.update(parentId, childId, request);
    }

    @DeleteMapping("/{childId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentParentId UUID parentId, @PathVariable UUID childId) {
        childService.delete(parentId, childId);
    }
}
