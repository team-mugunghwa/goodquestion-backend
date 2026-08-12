package com.mugunghwa.goodquestion.learning.reward.planet;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlacementCreateRequest;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlacementMoveRequest;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlacementResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 격자 배치 3종 — 놓기·옮기기·치우기(보상-17).
 *
 * <p>되돌리기는 별도 API가 없다. 클라이언트가 직전 조작의 역조작을 호출한다(보상-18).
 * 겹침은 놓기 전 확인과 DB UNIQUE 제약으로 막고 위반은 409(CELL_OCCUPIED)로 알린다.
 */
@RestController
@RequiredArgsConstructor
public class PlacementController {

    private final PlanetService planetService;

    @PostMapping("/api/children/{childId}/planet/placements")
    @ResponseStatus(HttpStatus.CREATED)
    public PlacementResponse place(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                                   @Valid @RequestBody PlacementCreateRequest request) {
        return planetService.place(parentId, childId, request);
    }

    @PatchMapping("/api/planet/placements/{placementId}")
    public PlacementResponse move(@CurrentParentId UUID parentId, @PathVariable UUID placementId,
                                  @Valid @RequestBody PlacementMoveRequest request) {
        return planetService.move(parentId, placementId, request);
    }

    /** 치우기는 삭제가 아니라 보관함 복귀다(보상-02, 보상-17). */
    @DeleteMapping("/api/planet/placements/{placementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@CurrentParentId UUID parentId, @PathVariable UUID placementId) {
        planetService.remove(parentId, placementId);
    }
}
