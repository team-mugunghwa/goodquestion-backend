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

/** 내 행성 조회·이름 변경·튜토리얼(보상-15~16, 21~23, 26). TODO: Planet 엔티티와 서비스 구현. */
@RestController
@RequestMapping("/api/children/{childId}/planet")
@RequiredArgsConstructor
public class PlanetController {

    @GetMapping
    public PlanetResponse getPlanet(@CurrentParentId UUID parentId, @PathVariable UUID childId) {
        throw new UnsupportedOperationException("미구현: 내 행성 조회");
    }

    @PatchMapping
    public PlanetRenameResponse rename(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                                       @Valid @RequestBody PlanetRenameRequest request) {
        throw new UnsupportedOperationException("미구현: 행성 이름 변경");
    }

    /** 최초 진입 시 배치 1회 따라 하기 안내를 마쳤음을 기록한다(보상-22). */
    @PostMapping("/tutorial-complete")
    public TutorialCompleteResponse completeTutorial(@CurrentParentId UUID parentId,
                                                     @PathVariable UUID childId) {
        throw new UnsupportedOperationException("미구현: 행성 튜토리얼 완료");
    }
}
