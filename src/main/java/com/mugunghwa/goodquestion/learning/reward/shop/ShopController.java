package com.mugunghwa.goodquestion.learning.reward.shop;

import com.mugunghwa.goodquestion.global.idempotency.IdempotencyService;
import com.mugunghwa.goodquestion.global.idempotency.IdempotentEndpoint;
import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.learning.reward.shop.dto.ChildItemResponse;
import com.mugunghwa.goodquestion.learning.reward.shop.dto.ItemPurchaseRequest;
import com.mugunghwa.goodquestion.learning.reward.shop.dto.ItemPurchaseResponse;
import com.mugunghwa.goodquestion.learning.reward.shop.dto.ShopItemResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** 상점과 보관함(보상-09~14). */
@RestController
@RequestMapping("/api/children/{childId}")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final IdempotencyService idempotencyService;

    /** 해금·실루엣·구매 가능·부족 수량을 서버가 계산해 노출 순서대로 내려준다. */
    @GetMapping("/shop/items")
    public List<ShopItemResponse> getShopItems(@CurrentParentId UUID parentId,
                                               @PathVariable UUID childId) {
        return shopService.getShopItems(parentId, childId);
    }

    /**
     * 해금·잔액 검증 → 차감 + 이력 + 보유 등록을 단일 트랜잭션으로 처리한다(보상-14).
     *
     * <p>Idempotency-Key 헤더(선택)를 실으면 타임아웃 후 같은 키의 재전송이 이중
     * 차감이 되지 않는다 - 완료된 구매면 저장된 응답을 재생한다.
     */
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemPurchaseResponse purchase(@CurrentParentId UUID parentId,
                                         @PathVariable UUID childId,
                                         @RequestHeader(value = "Idempotency-Key", required = false)
                                         String idempotencyKey,
                                         @Valid @RequestBody ItemPurchaseRequest request) {
        return idempotencyService.execute(IdempotentEndpoint.ITEM_PURCHASE, childId, parentId,
                idempotencyKey, ItemPurchaseResponse.class,
                () -> shopService.purchase(parentId, childId, request));
    }

    /** placed 생략 시 전체, false면 보관함만. */
    @GetMapping("/items")
    public List<ChildItemResponse> getOwnedItems(@CurrentParentId UUID parentId,
                                                 @PathVariable UUID childId,
                                                 @RequestParam(required = false) Boolean placed) {
        return shopService.getChildItems(parentId, childId, placed);
    }
}
