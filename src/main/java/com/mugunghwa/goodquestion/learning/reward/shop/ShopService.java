package com.mugunghwa.goodquestion.learning.reward.shop;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.learning.reward.planet.PlanetItemRepository;
import com.mugunghwa.goodquestion.learning.reward.shop.dto.ChildItemResponse;
import com.mugunghwa.goodquestion.learning.reward.shop.dto.ItemPurchaseRequest;
import com.mugunghwa.goodquestion.learning.reward.shop.dto.ItemPurchaseResponse;
import com.mugunghwa.goodquestion.learning.reward.shop.dto.ShopItemResponse;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustService;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWallet;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWalletRepository;
import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.child.ChildService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 상점과 보관함 (보상-09~14, 보상-20).
 *
 * <p>해금·구매 가능 여부·부족 수량은 전부 서버가 계산해 내린다. 가격 규칙이 화면에도 있으면
 * 두 값이 어긋날 때 아이가 살 수 있다고 본 것을 서버가 거절한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopService {

    private final ItemRepository itemRepository;
    private final ItemCatalog itemCatalog;
    private final ChildItemRepository childItemRepository;
    private final PlanetItemRepository planetItemRepository;
    private final StardustWalletRepository walletRepository;
    private final StardustService stardustService;
    private final ItemUnlockPolicy unlockPolicy;
    private final ChildService childService;
    private final EntityManager entityManager;

    public List<ShopItemResponse> getShopItems(UUID parentId, UUID childId) {
        childService.getOwnedChild(parentId, childId);
        StardustWallet wallet = getWalletOf(childId);

        return itemCatalog.activeItems().stream()
                .map(item -> toShopItem(item, childId, wallet))
                .toList();
    }

    /**
     * 아이템 구매. 해금 -> 잔액 -> 차감과 입고를 한 트랜잭션에서 끝낸다(데이터-08).
     *
     * <p>같은 아이템을 또 사는 것은 막지 않는다. 같은 나무를 여러 그루 심는 것이 정상 사용이다.
     */
    @Transactional
    public ItemPurchaseResponse purchase(UUID parentId, UUID childId, ItemPurchaseRequest request) {
        Child child = childService.getOwnedChild(parentId, childId);
        Item item = itemRepository.findById(request.itemId())
                .filter(found -> found.getStatus() == ItemStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "아이템을 찾을 수 없습니다."));

        StardustWallet wallet = getWalletOf(childId);
        if (!unlockPolicy.isUnlocked(item, childId, wallet.getTotalEarned())) {
            throw new BusinessException(ErrorCode.ITEM_LOCKED);
        }

        StardustWallet charged = stardustService.spendForPurchase(childId, item);
        ChildItem acquired = childItemRepository.saveAndFlush(
                ChildItem.builder().child(child).item(item).build());

        // acquired_at은 DB 기본값이라 저장 직후 엔티티에는 비어 있다. 응답이 그 값을 담으므로 되읽는다.
        entityManager.refresh(acquired);
        return new ItemPurchaseResponse(toChildItem(acquired, false), charged.getBalance());
    }

    /** placed가 null이면 전체. false면 보관함(=아직 행성에 놓지 않은 것). */
    public List<ChildItemResponse> getChildItems(UUID parentId, UUID childId, Boolean placed) {
        childService.getOwnedChild(parentId, childId);

        Set<UUID> placedIds = planetItemRepository.findPlacedChildItemIds(childId);
        return childItemRepository.findAllByChildId(childId).stream()
                .map(childItem -> toChildItem(childItem, placedIds.contains(childItem.getId())))
                .filter(response -> placed == null || response.placed() == placed)
                .toList();
    }

    private ShopItemResponse toShopItem(ItemView item, UUID childId, StardustWallet wallet) {
        boolean unlocked = unlockPolicy.isUnlocked(item, childId, wallet.getTotalEarned());
        int shortfall = Math.max(0, item.price() - wallet.getBalance());

        return new ShopItemResponse(
                item.id(), item.name(), item.category(), item.price(),
                item.modelUrl(), item.thumbnailUrl(),
                unlocked,
                // 잠긴 아이템은 실루엣으로 보여 준다. 무엇이 더 있는지 보여야 모으고 싶어진다.
                !unlocked,
                unlockGuideOf(item),
                unlocked && shortfall == 0,
                shortfall);
    }

    /** 이야기 완주로 열리는 아이템만 어떤 이야기인지 함께 안내한다. */
    private ShopItemResponse.UnlockGuide unlockGuideOf(ItemView item) {
        if (item.unlockType() != UnlockType.STORY_COMPLETE || item.unlockStoryTitle() == null) {
            return null;
        }
        return new ShopItemResponse.UnlockGuide(
                item.unlockStoryTitle(), item.unlockStoryImageUrl());
    }

    private ChildItemResponse toChildItem(ChildItem childItem, boolean placed) {
        Item item = childItem.getItem();
        return new ChildItemResponse(
                childItem.getId(), item.getId(), item.getName(), item.getCategory(),
                item.getThumbnailUrl(), item.getModelUrl(), childItem.getAcquiredAt(), placed);
    }

    private StardustWallet getWalletOf(UUID childId) {
        return walletRepository.findByChildId(childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "별가루 지갑을 찾을 수 없습니다."));
    }
}
