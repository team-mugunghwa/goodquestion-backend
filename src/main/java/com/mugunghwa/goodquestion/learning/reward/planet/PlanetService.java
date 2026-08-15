package com.mugunghwa.goodquestion.learning.reward.planet;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlacementCreateRequest;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlacementMoveRequest;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlacementResponse;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlanetRenameRequest;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlanetRenameResponse;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.PlanetResponse;
import com.mugunghwa.goodquestion.learning.reward.planet.dto.TutorialCompleteResponse;
import com.mugunghwa.goodquestion.learning.reward.shop.ChildItem;
import com.mugunghwa.goodquestion.learning.reward.shop.ChildItemRepository;
import com.mugunghwa.goodquestion.learning.reward.shop.Item;
import com.mugunghwa.goodquestion.learning.reward.shop.ItemRepository;
import com.mugunghwa.goodquestion.learning.reward.shop.ItemStatus;
import com.mugunghwa.goodquestion.learning.reward.shop.ItemUnlockPolicy;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWallet;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWalletRepository;
import com.mugunghwa.goodquestion.user.child.ChildService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 내 행성과 격자 배치 (보상-15~23, 26).
 *
 * <p>배치는 행 단위 조작이다. 스냅샷을 통째로 저장하지 않고 놓기·옮기기·치우기를 각각 받는다.
 * 통째로 저장하면 마지막 요청이 그 사이의 다른 변경을 조용히 덮는다.
 *
 * <p>판 크기와 모양은 서버에 두지 않는다. 클라이언트 카탈로그가 단일 소스이고, 서버가 격자를
 * 따로 들고 있으면 두 값이 어긋날 때 배치가 이유 없이 거부된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanetService {

    private final PlanetRepository planetRepository;
    private final PlanetItemRepository planetItemRepository;
    private final ChildItemRepository childItemRepository;
    private final ItemRepository itemRepository;
    private final StardustWalletRepository walletRepository;
    private final ItemUnlockPolicy unlockPolicy;
    private final ChildService childService;

    public PlanetResponse getPlanet(UUID parentId, UUID childId) {
        childService.getOwnedChild(parentId, childId);
        Planet planet = getPlanetOf(childId);

        return new PlanetResponse(
                planet.getId(), planet.getName(), planet.isTutorialCompleted(),
                planetItemRepository.findAllByPlanetId(planet.getId()).stream()
                        .map(PlanetService::toPlacement).toList(),
                progressOf(childId, planet));
    }

    @Transactional
    public PlanetRenameResponse rename(UUID parentId, UUID childId, PlanetRenameRequest request) {
        childService.getOwnedChild(parentId, childId);
        Planet planet = getPlanetOf(childId);
        planet.rename(request.name());
        return new PlanetRenameResponse(planet.getId(), planet.getName());
    }

    @Transactional
    public TutorialCompleteResponse completeTutorial(UUID parentId, UUID childId) {
        childService.getOwnedChild(parentId, childId);
        Planet planet = getPlanetOf(childId);
        planet.completeTutorial();
        return new TutorialCompleteResponse(planet.isTutorialCompleted());
    }

    /** 보관함의 아이템을 빈 칸에 놓는다(보상-17). */
    @Transactional
    public PlacementResponse place(UUID parentId, UUID childId, PlacementCreateRequest request) {
        childService.getOwnedChild(parentId, childId);
        Planet planet = getPlanetOf(childId);

        ChildItem childItem = childItemRepository
                .findByIdAndChildId(request.childItemId(), childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "보유 아이템을 찾을 수 없습니다."));
        if (planetItemRepository.existsByChildItemId(childItem.getId())) {
            throw new BusinessException(ErrorCode.ITEM_ALREADY_PLACED);
        }

        short q = toCoordinate(request.placedQ());
        short r = toCoordinate(request.placedR());
        requireInsideGrid(q, r);
        requireEmptyCell(planet, q, r);

        return toPlacement(planetItemRepository.save(PlanetItem.builder()
                .planet(planet).childItem(childItem).placedQ(q).placedR(r).build()));
    }

    /** 놓인 아이템을 다른 칸으로 옮긴다(보상-18). */
    @Transactional
    public PlacementResponse move(UUID parentId, UUID placementId, PlacementMoveRequest request) {
        PlanetItem placement = getOwnedPlacement(parentId, placementId);

        short q = toCoordinate(request.placedQ());
        short r = toCoordinate(request.placedR());
        requireInsideGrid(q, r);
        if (placement.getPlacedQ() != q || placement.getPlacedR() != r) {
            requireEmptyCell(placement.getPlanet(), q, r);
        }

        placement.moveTo(q, r);
        return toPlacement(placement);
    }

    /**
     * 놓인 아이템을 치운다(보상-19).
     *
     * <p>배치 행만 지우고 보유 아이템은 남는다 — 보관함으로 돌아가는 것이지 잃는 것이 아니다.
     */
    @Transactional
    public void remove(UUID parentId, UUID placementId) {
        planetItemRepository.delete(getOwnedPlacement(parentId, placementId));
    }

    /**
     * 다음 해금 목표 — 잠긴 것 중 진열 순서가 가장 앞선 하나만 보여 준다.
     *
     * <p>전부 보여 주면 아이가 무엇을 향해 모으는지 흐려진다. 모두 열렸으면 null이다.
     */
    private PlanetResponse.Progress progressOf(UUID childId, Planet planet) {
        StardustWallet wallet = walletRepository.findByChildId(childId).orElse(null);
        int totalEarned = wallet == null ? 0 : wallet.getTotalEarned();

        PlanetResponse.NextUnlock nextUnlock = itemRepository
                .findAllByStatusOrderByDisplayOrderAsc(ItemStatus.ACTIVE).stream()
                .filter(item -> !unlockPolicy.isUnlocked(item, childId, totalEarned))
                .findFirst()
                .map(this::toNextUnlock)
                .orElse(null);

        return new PlanetResponse.Progress(
                planetItemRepository.countByPlanetId(planet.getId()), nextUnlock);
    }

    private PlanetResponse.NextUnlock toNextUnlock(Item item) {
        return new PlanetResponse.NextUnlock(
                item.getName(), item.getThumbnailUrl(), unlockPolicy.conditionText(item));
    }

    private void requireEmptyCell(Planet planet, short q, short r) {
        if (planetItemRepository.existsByPlanetIdAndPlacedQAndPlacedR(planet.getId(), q, r)) {
            throw new BusinessException(ErrorCode.CELL_OCCUPIED);
        }
    }

    /**
     * 축좌표는 원점 기준이라 음수가 유효해서 하한을 두지 않는다. 다만 컬럼이 smallint라
     * 그 범위를 벗어난 값은 애초에 저장할 수 없어 여기서 막는다.
     */
    private short toCoordinate(Integer value) {
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw new BusinessException(ErrorCode.GRID_OUT_OF_RANGE);
        }
        return value.shortValue();
    }

    private PlanetItem getOwnedPlacement(UUID parentId, UUID placementId) {
        PlanetItem placement = planetItemRepository.findById(placementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "배치를 찾을 수 없습니다."));
        if (!placement.getPlanet().getChild().isOwnedBy(parentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return placement;
    }

    private Planet getPlanetOf(UUID childId) {
        return planetRepository.findByChildId(childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "행성을 찾을 수 없습니다."));
    }

    private static PlacementResponse toPlacement(PlanetItem placement) {
        Item item = placement.getChildItem().getItem();
        return new PlacementResponse(
                placement.getId(), placement.getChildItem().getId(), item.getId(),
                item.getModelUrl(), placement.getPlacedQ(), placement.getPlacedR());
    }

    /**
     * 섬 격자 안인지 검사한다(08-15 요청 #1).
     *
     * <p>기존 {@code toCoordinate}는 short 범위만 봐서 (99,99)도 (32767,32767)도 통과했다.
     * 그렇게 놓인 아이템은 화면에도 보관함에도 없고 {@code planet_items.child_item_id}가
     * UNIQUE라 다시 놓을 수도 없다 — <b>별가루를 주고 산 것이 영구히 사라진다.</b>
     *
     * <p>섬은 원형으로 깎은 정사각 격자다. 클라이언트가 {@code hypot(q,r) <= 8 + 0.35}로
     * 칸을 만들므로(221칸) 여기도 같은 원이어야 한다. 사각·육각식으로 막으면 클라이언트가
     * 실제로 쓰는 칸이 거부되거나((6,3)) 섬 밖이 통과된다((8,-8)).
     */
    private static final int GRID_RADIUS = 8;
    private static final double GRID_SLACK = 0.35;

    private void requireInsideGrid(short q, short r) {
        double limit = (GRID_RADIUS + GRID_SLACK) * (GRID_RADIUS + GRID_SLACK);
        if ((double) q * q + (double) r * r > limit) {
            throw new BusinessException(ErrorCode.GRID_OUT_OF_RANGE);
        }
    }
}
