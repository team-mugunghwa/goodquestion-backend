package com.mugunghwa.goodquestion.learning.reward.shop;

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

/** 상점과 보관함(보상-09~14). TODO: Item·ChildItem 엔티티와 해금 계산·구매 트랜잭션 구현. */
@RestController
@RequestMapping("/api/children/{childId}")
@RequiredArgsConstructor
public class ShopController {

    /** 해금·실루엣·구매 가능·부족 수량을 서버가 계산해 노출 순서대로 내려준다. */
    @GetMapping("/shop/items")
    public List<ShopItemResponse> getShopItems(@CurrentParentId UUID parentId,
                                               @PathVariable UUID childId) {
        throw new UnsupportedOperationException("미구현: 상점 목록 조회");
    }

    /** 해금·잔액 검증 → 차감 + 이력 + 보유 등록을 단일 트랜잭션으로 처리한다(보상-14). */
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemPurchaseResponse purchase(@CurrentParentId UUID parentId,
                                         @PathVariable UUID childId,
                                         @Valid @RequestBody ItemPurchaseRequest request) {
        throw new UnsupportedOperationException("미구현: 아이템 구매");
    }

    /** placed 생략 시 전체, false면 보관함만. */
    @GetMapping("/items")
    public List<ChildItemResponse> getOwnedItems(@CurrentParentId UUID parentId,
                                                 @PathVariable UUID childId,
                                                 @RequestParam(required = false) Boolean placed) {
        throw new UnsupportedOperationException("미구현: 보유 아이템(보관함) 조회");
    }
}
