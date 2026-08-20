package com.mugunghwa.goodquestion.learning.reward.shop;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.learning.reward.shop.dto.ChildItemResponse;
import com.mugunghwa.goodquestion.learning.reward.shop.dto.ItemPurchaseRequest;
import com.mugunghwa.goodquestion.learning.reward.shop.dto.ItemPurchaseResponse;
import com.mugunghwa.goodquestion.learning.reward.shop.dto.ShopItemResponse;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWalletRepository;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.mugunghwa.goodquestion.learning.reward.RewardFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
class ShopServiceTest {

    @Autowired
    private ShopService shopService;

    @Autowired
    private StardustWalletRepository walletRepository;

    @Autowired
    private ItemRepository itemRepository;

    /**
     * 이 테스트가 보는 것은 <b>전부 돌려주는가</b>이지 몇 개인가가 아니다.
     *
     * <p>예전에는 개수를 숫자로 박아 뒀는데({@code hasSize(45)}), 시드에 아이템이 더해질
     * 때마다 이 테스트가 깨졌다 - 기념 아이템 3종이 들어오면서(#124) 실제로 깨진 채로
     * develop 에 머지됐다. 콘텐츠가 늘었다고 실패하는 테스트는 늘 실패하는 테스트가 되고,
     * 그러면 아무도 안 본다.
     *
     * <p>그래서 기대값을 저장소에서 가져온다. 상점은 캐시({@code ItemCatalog})를 거치고
     * 이쪽은 안 거치므로, 캐시가 몇 개를 흘리거나 순서를 잃으면 여전히 잡힌다.
     * 첫 칸이 돌인 것만 따로 확인하는 이유는 시드의 진열 순서 자체가 뒤집히는 경우를
     * 잡기 위해서다 - 저장소도 같이 뒤집히면 위 비교로는 안 잡힌다.
     */
    @Test
    void 상점은_진열_순서대로_전체를_돌려준다() {
        List<UUID> displayed = itemRepository.findAllActiveWithUnlockStory(ItemStatus.ACTIVE)
                .stream().map(Item::getId).toList();

        List<ShopItemResponse> items = shopService.getShopItems(PARENT_ID, CHILD_ID);

        // 개수와 순서를 한 번에 본다. 첫 칸만 보면 정렬이 통째로 빠져도 우연히 맞는다.
        assertThat(items).extracting(ShopItemResponse::itemId)
                .containsExactlyElementsOf(displayed);
        assertThat(items.getFirst().itemId()).isEqualTo(ROCK_ITEM_ID);
    }

    @Test
    void 항상_열린_아이템은_해금_상태다() {
        ShopItemResponse rock = find(shopService.getShopItems(PARENT_ID, CHILD_ID), ROCK_ITEM_ID);

        assertThat(rock.unlocked()).isTrue();
        assertThat(rock.silhouette()).isFalse();
        assertThat(rock.unlockGuide()).isNull();
    }

    @Test
    void 누적_획득량이_기준을_넘으면_해금된다() {
        // 지우는 누적 25 - 우리 집(9)도 여우(11)도 열린다. 하준은 누적 0 - 잠기고 실루엣이다
        List<ShopItemResponse> items = shopService.getShopItems(PARENT_ID, CHILD_ID);
        assertThat(find(items, HOUSE_ITEM_ID).unlocked()).isTrue();
        assertThat(find(items, FOX_ITEM_ID).unlocked()).isTrue();

        List<ShopItemResponse> siblingItems = shopService.getShopItems(PARENT_ID, SIBLING_ID);
        assertThat(find(siblingItems, FOX_ITEM_ID).unlocked()).isFalse();
        assertThat(find(siblingItems, FOX_ITEM_ID).silhouette()).isTrue();
    }

    @Test
    void 잔액이_아니라_누적_획득량으로_판정한다() {
        // 지우의 잔액(14)을 여우 해금 기준(11) 아래로 써 버려도 누적(25) 기반 해금은 유지된다
        shopService.purchase(PARENT_ID, CHILD_ID, new ItemPurchaseRequest(HOUSE_ITEM_ID));
        shopService.purchase(PARENT_ID, CHILD_ID, new ItemPurchaseRequest(BUNNY_ITEM_ID));
        shopService.purchase(PARENT_ID, CHILD_ID, new ItemPurchaseRequest(CAT_ITEM_ID));
        shopService.purchase(PARENT_ID, CHILD_ID, new ItemPurchaseRequest(FOX_ITEM_ID));
        assertThat(walletRepository.findByChildId(CHILD_ID).orElseThrow().getBalance()).isLessThan(11);

        assertThat(find(shopService.getShopItems(PARENT_ID, CHILD_ID), FOX_ITEM_ID).unlocked()).isTrue();
    }

    @Test
    void 이야기_완주로_열리는_아이템은_안내를_함께_준다() {
        ShopItemResponse bunny = find(shopService.getShopItems(PARENT_ID, CHILD_ID), BUNNY_ITEM_ID);

        assertThat(bunny.unlocked()).isTrue();   // 지우는 방귀 이야기를 완주했다
        assertThat(bunny.unlockGuide()).isNotNull();
        assertThat(bunny.unlockGuide().storyTitle()).isEqualTo("방귀 뀌는 며느리");
    }

    @Test
    void 완주하지_않은_아이에게는_동물이_잠겨_있다() {
        ShopItemResponse bunny = find(shopService.getShopItems(PARENT_ID, SIBLING_ID), BUNNY_ITEM_ID);

        assertThat(bunny.unlocked()).isFalse();
        assertThat(bunny.unlockGuide()).isNotNull();
    }

    @Test
    void 부족한_별가루를_계산해_내려준다() {
        // 지우 잔액 14, 우리 집 가격 2 - 살 수 있다
        ShopItemResponse house = find(shopService.getShopItems(PARENT_ID, CHILD_ID), HOUSE_ITEM_ID);
        assertThat(house.purchasable()).isTrue();
        assertThat(house.shortfall()).isZero();

        // 하준 잔액 0, 작은 돌 가격 1
        ShopItemResponse rock = find(shopService.getShopItems(PARENT_ID, SIBLING_ID), ROCK_ITEM_ID);
        assertThat(rock.purchasable()).isFalse();
        assertThat(rock.shortfall()).isEqualTo(1);
    }

    @Test
    void 구매하면_차감되고_보관함에_들어온다() {
        int before = walletRepository.findByChildId(CHILD_ID).orElseThrow().getBalance();

        ItemPurchaseResponse response =
                shopService.purchase(PARENT_ID, CHILD_ID, new ItemPurchaseRequest(ROCK_ITEM_ID));

        assertThat(response.balance()).isEqualTo(before - 1);
        assertThat(response.item().placed()).isFalse();
        assertThat(response.item().acquiredAt()).isNotNull();
        assertThat(shopService.getChildItems(PARENT_ID, CHILD_ID, false))
                .extracting(ChildItemResponse::childItemId)
                .contains(response.item().childItemId());
    }

    @Test
    void 누적_획득량은_구매로_줄지_않는다() {
        int totalEarnedBefore = walletRepository.findByChildId(CHILD_ID).orElseThrow().getTotalEarned();

        shopService.purchase(PARENT_ID, CHILD_ID, new ItemPurchaseRequest(ROCK_ITEM_ID));

        assertThat(walletRepository.findByChildId(CHILD_ID).orElseThrow().getTotalEarned())
                .isEqualTo(totalEarnedBefore);
    }

    @Test
    void 같은_아이템을_또_살_수_있다() {
        ItemPurchaseResponse first =
                shopService.purchase(PARENT_ID, CHILD_ID, new ItemPurchaseRequest(ROCK_ITEM_ID));
        ItemPurchaseResponse second =
                shopService.purchase(PARENT_ID, CHILD_ID, new ItemPurchaseRequest(ROCK_ITEM_ID));

        assertThat(second.item().childItemId()).isNotEqualTo(first.item().childItemId());
    }

    @Test
    void 잠긴_아이템은_살_수_없다() {
        // 하준은 누적 0이라 여우(누적 11)가 잠겨 있다. 해금 검증이 잔액 검증보다 먼저다.
        assertThatThrownBy(() ->
                shopService.purchase(PARENT_ID, SIBLING_ID, new ItemPurchaseRequest(FOX_ITEM_ID)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ITEM_LOCKED);
    }

    @Test
    void 별가루가_모자라면_살_수_없다() {
        // 하준은 잔액 0이고 돌은 항상 열려 있다 - 해금이 아니라 잔액에서 막혀야 한다
        assertThatThrownBy(() ->
                shopService.purchase(PARENT_ID, SIBLING_ID, new ItemPurchaseRequest(ROCK_ITEM_ID)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STARDUST_INSUFFICIENT);
    }

    @Test
    void 보관함은_놓지_않은_것만_돌려준다() {
        List<ChildItemResponse> stored = shopService.getChildItems(PARENT_ID, CHILD_ID, false);
        List<ChildItemResponse> placed = shopService.getChildItems(PARENT_ID, CHILD_ID, true);

        assertThat(stored).extracting(ChildItemResponse::childItemId).contains(STORED_CHILD_ITEM_ID);
        assertThat(placed).extracting(ChildItemResponse::childItemId).doesNotContain(STORED_CHILD_ITEM_ID);
        assertThat(shopService.getChildItems(PARENT_ID, CHILD_ID, null))
                .hasSize(stored.size() + placed.size());
    }

    @Test
    void 남의_아이_상점은_볼_수_없다() {
        assertThatThrownBy(() -> shopService.getShopItems(OTHER_PARENT_ID, CHILD_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    private ShopItemResponse find(List<ShopItemResponse> items, java.util.UUID itemId) {
        return items.stream().filter(item -> item.itemId().equals(itemId)).findFirst().orElseThrow();
    }
}
