package com.mugunghwa.goodquestion.learning.reward.planet;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlacementCreateRequest;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlacementMoveRequest;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlacementResponse;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlanetRenameRequest;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlanetResponse;
import com.mugunghwa.goodquestion.learning.reward.shop.ChildItemRepository;
import com.mugunghwa.goodquestion.learning.reward.shop.ShopService;
import com.mugunghwa.goodquestion.learning.reward.shop.dto.ChildItemResponse;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static com.mugunghwa.goodquestion.learning.reward.RewardFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class PlanetServiceTest {

    @Autowired
    private PlanetService planetService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private ChildItemRepository childItemRepository;

    @Test
    void 행성은_이름과_놓인_아이템을_돌려준다() {
        PlanetResponse planet = planetService.getPlanet(PARENT_ID, CHILD_ID);

        assertThat(planet.name()).isEqualTo("지우의 행성");
        assertThat(planet.tutorialCompleted()).isTrue();
        assertThat(planet.placedItems()).hasSize(2);
        assertThat(planet.progress().placedCount()).isEqualTo(2);
    }

    @Test
    void 모든_아이템이_열리면_다음_해금_목표가_없다() {
        // 지우는 누적 25에 완주 이력이 있어 전 아이템이 열려 있다 (2026-08 해금 인하 이후)
        PlanetResponse.NextUnlock nextUnlock = planetService.getPlanet(PARENT_ID, CHILD_ID)
                .progress().nextUnlock();

        assertThat(nextUnlock).isNull();
    }

    @Test
    void 완주로_열리는_아이템은_이야기_이름으로_안내한다() {
        PlanetResponse.NextUnlock nextUnlock = planetService.getPlanet(PARENT_ID, SIBLING_ID)
                .progress().nextUnlock();

        // 하준은 누적 0이라 진열 순서상 집(누적 3)이 가장 앞선 잠금이다.
        assertThat(nextUnlock.itemName()).isEqualTo("집");
        assertThat(nextUnlock.conditionText()).isEqualTo("별가루를 모두 3개 모으면 열려요");
    }

    @Test
    void 행성_이름을_바꿀_수_있다() {
        planetService.rename(PARENT_ID, CHILD_ID, new PlanetRenameRequest("별똥별 행성"));

        assertThat(planetService.getPlanet(PARENT_ID, CHILD_ID).name()).isEqualTo("별똥별 행성");
    }

    @Test
    void 튜토리얼을_마쳤다고_기록한다() {
        assertThat(planetService.getPlanet(PARENT_ID, SIBLING_ID).tutorialCompleted()).isFalse();

        planetService.completeTutorial(PARENT_ID, SIBLING_ID);

        assertThat(planetService.getPlanet(PARENT_ID, SIBLING_ID).tutorialCompleted()).isTrue();
    }

    @Test
    void 보관함의_아이템을_빈_칸에_놓는다() {
        PlacementResponse placement = planetService.place(PARENT_ID, CHILD_ID,
                new PlacementCreateRequest(STORED_CHILD_ITEM_ID, 2, 2));

        assertThat(placement.placedQ()).isEqualTo(2);
        assertThat(placement.childItemId()).isEqualTo(STORED_CHILD_ITEM_ID);
        assertThat(planetService.getPlanet(PARENT_ID, CHILD_ID).placedItems()).hasSize(3);
    }

    @Test
    void 음수_좌표에도_놓을_수_있다() {
        PlacementResponse placement = planetService.place(PARENT_ID, CHILD_ID,
                new PlacementCreateRequest(STORED_CHILD_ITEM_ID, -3, -4));

        assertThat(placement.placedQ()).isEqualTo(-3);
        assertThat(placement.placedR()).isEqualTo(-4);
    }

    @Test
    void 좌표가_저장_범위를_벗어나면_막는다() {
        assertThatThrownBy(() -> planetService.place(PARENT_ID, CHILD_ID,
                new PlacementCreateRequest(STORED_CHILD_ITEM_ID, 40_000, 0)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.GRID_OUT_OF_RANGE);
    }

    @Test
    void 이미_찬_칸에는_놓을_수_없다() {
        // 시드에서 돌이 (0,0)에 놓여 있다
        assertThatThrownBy(() -> planetService.place(PARENT_ID, CHILD_ID,
                new PlacementCreateRequest(STORED_CHILD_ITEM_ID, 0, 0)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CELL_OCCUPIED);
    }

    @Test
    void 이미_놓인_아이템은_또_놓을_수_없다() {
        java.util.UUID placedChildItemId = planetService.getPlanet(PARENT_ID, CHILD_ID)
                .placedItems().getFirst().childItemId();

        assertThatThrownBy(() -> planetService.place(PARENT_ID, CHILD_ID,
                new PlacementCreateRequest(placedChildItemId, 5, 5)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITEM_ALREADY_PLACED);
    }

    @Test
    void 형제의_아이템은_놓을_수_없다() {
        assertThatThrownBy(() -> planetService.place(PARENT_ID, SIBLING_ID,
                new PlacementCreateRequest(STORED_CHILD_ITEM_ID, 5, 5)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void 놓인_아이템을_다른_칸으로_옮긴다() {
        // 섬은 원형으로 깎은 격자라 모서리는 칸이 아니다. 원래 쓰던 (7,-7)은
        // hypot 9.90 > 8.35라 클라이언트가 만들지 않는 좌표여서 (7,-3)으로 옮겼다.
        PlacementResponse moved = planetService.move(PARENT_ID, PLACED_AT_ORIGIN_ID,
                new PlacementMoveRequest(7, -3));

        assertThat(moved.placedQ()).isEqualTo(7);
        assertThat(moved.placedR()).isEqualTo(-3);
    }

    @Test
    void 제자리로_옮기는_것은_충돌이_아니다() {
        PlacementResponse moved = planetService.move(PARENT_ID, PLACED_AT_ORIGIN_ID,
                new PlacementMoveRequest(0, 0));

        assertThat(moved.placedQ()).isZero();
    }

    @Test
    void 찬_칸으로는_옮길_수_없다() {
        // 시드의 다른 배치가 (1,-1)에 있다
        assertThatThrownBy(() -> planetService.move(PARENT_ID, PLACED_AT_ORIGIN_ID,
                new PlacementMoveRequest(1, -1)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CELL_OCCUPIED);
    }

    @Test
    void 치우면_보관함으로_돌아온다() {
        java.util.UUID childItemId = planetService.getPlanet(PARENT_ID, CHILD_ID)
                .placedItems().stream()
                .filter(placement -> placement.placementId().equals(PLACED_AT_ORIGIN_ID))
                .findFirst().orElseThrow().childItemId();

        planetService.remove(PARENT_ID, PLACED_AT_ORIGIN_ID);

        assertThat(childItemRepository.findById(childItemId)).isPresent();
        assertThat(shopService.getChildItems(PARENT_ID, CHILD_ID, false))
                .extracting(ChildItemResponse::childItemId)
                .contains(childItemId);
    }

    @Test
    void 남의_배치는_옮기거나_치울_수_없다() {
        assertThatThrownBy(() -> planetService.move(OTHER_PARENT_ID, PLACED_AT_ORIGIN_ID,
                new PlacementMoveRequest(3, 3)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);

        assertThatThrownBy(() -> planetService.remove(OTHER_PARENT_ID, PLACED_AT_ORIGIN_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }
}
