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
 * 겹침은 DB UNIQUE 제약으로 막고 위반은 409(CELL_OCCUPIED)로 변환한다.
 * TODO: Planet·PlanetItem 엔티티와 서비스 구현.
 */
@RestController
@RequiredArgsConstructor
public class PlacementController {

    @PostMapping("/api/children/{childId}/planet/placements")
    @ResponseStatus(HttpStatus.CREATED)
    public PlacementResponse place(@CurrentParentId UUID parentId, @PathVariable UUID childId,
                                   @Valid @RequestBody PlacementCreateRequest request) {
        throw new UnsupportedOperationException("미구현: 아이템 배치(놓기)");
    }

    @PatchMapping("/api/planet/placements/{placementId}")
    public PlacementResponse move(@CurrentParentId UUID parentId, @PathVariable UUID placementId,
                                  @Valid @RequestBody PlacementMoveRequest request) {
        throw new UnsupportedOperationException("미구현: 배치 이동(옮기기)");
    }

    /** 치우기는 삭제가 아니라 보관함 복귀다(보상-02, 보상-17). */
    @DeleteMapping("/api/planet/placements/{placementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@CurrentParentId UUID parentId, @PathVariable UUID placementId) {
        throw new UnsupportedOperationException("미구현: 배치 회수(치우기)");
    }
}
