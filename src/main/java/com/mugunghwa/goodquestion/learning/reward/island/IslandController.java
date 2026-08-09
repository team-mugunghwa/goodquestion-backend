package com.mugunghwa.goodquestion.learning.reward.island;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.learning.reward.island.dto.IslandRenameRequest;
import com.mugunghwa.goodquestion.learning.reward.island.dto.IslandRenameResponse;
import com.mugunghwa.goodquestion.learning.reward.island.dto.IslandResponse;
import com.mugunghwa.goodquestion.learning.reward.island.dto.TutorialCompleteResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** 내 섬 조회·이름 변경·튜토리얼(보상-15~16, 21~23, 26). TODO: Island 엔티티와 서비스 구현. */
@RestController
@RequestMapping("/api/children/{childId}/island")
@RequiredArgsConstructor
public class IslandController {

    @GetMapping
    public IslandResponse getIsland(@CurrentParentId UUID parentId, @PathVariable UUID childId) {
        throw new UnsupportedOperationException("미구현: 내 섬 조회");
    }

    @PatchMapping
    public IslandRenameResponse rename(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                                       @Valid @RequestBody IslandRenameRequest request) {
        throw new UnsupportedOperationException("미구현: 섬 이름 변경");
    }

    /** 최초 진입 시 배치 1회 따라 하기 안내를 마쳤음을 기록한다(보상-22). */
    @PostMapping("/tutorial-complete")
    public TutorialCompleteResponse completeTutorial(@CurrentParentId UUID parentId,
                                                     @PathVariable UUID childId) {
        throw new UnsupportedOperationException("미구현: 섬 튜토리얼 완료");
    }
}
