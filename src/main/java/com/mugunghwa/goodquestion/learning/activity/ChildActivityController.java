package com.mugunghwa.goodquestion.learning.activity;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.learning.activity.dto.ChildActivityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** 마이페이지 활동 요약(완주 편수·별가루). */
@RestController
@RequestMapping("/api/children/{childId}/activity")
@RequiredArgsConstructor
public class ChildActivityController {

    private final ChildActivityService activityService;

    @GetMapping
    public ChildActivityResponse getActivity(@CurrentParentId UUID parentId,
                                             @PathVariable UUID childId) {
        return activityService.getActivity(parentId, childId);
    }
}