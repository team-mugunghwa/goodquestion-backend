package com.mugunghwa.goodquestion.home;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.home.dto.HomeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/children/{childId}/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    public HomeResponse getHome(@CurrentParentId UUID parentId, @PathVariable UUID childId) {
        return homeService.getHome(parentId, childId);
    }
}
