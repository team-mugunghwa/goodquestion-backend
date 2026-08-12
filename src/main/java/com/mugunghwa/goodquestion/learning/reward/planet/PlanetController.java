package com.mugunghwa.goodquestion.learning.reward.planet;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlanetRenameRequest;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlanetRenameResponse;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlanetResponse;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.TutorialCompleteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** 내 행성 조회·이름 변경·튜토리얼(보상-15~16, 21~23, 26). */
@RestController
@RequestMapping("/api/children/{childId}/planet")
@RequiredArgsConstructor
public class PlanetController {

    private final PlanetService planetService;

    @GetMapping
    public PlanetResponse getPlanet(@CurrentParentId UUID parentId, @PathVariable UUID childId) {
        return planetService.getPlanet(parentId, childId);
    }

    @PatchMapping
    public PlanetRenameResponse rename(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                                       @Valid @RequestBody PlanetRenameRequest request) {
        return planetService.rename(parentId, childId, request);
    }

    /** 최초 진입 시 배치 1회 따라 하기 안내를 마쳤음을 기록한다(보상-22). */
    @PostMapping("/tutorial-complete")
    public TutorialCompleteResponse completeTutorial(@CurrentParentId UUID parentId,
                                                     @PathVariable UUID childId) {
        return planetService.completeTutorial(parentId, childId);
    }
}
