package com.mugunghwa.goodquestion.learning.reward.shop;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    List<Item> findAllByOrderByDisplayOrderAsc();

    /** 운영 중 내린 아이템(HIDDEN)은 상점에 노출하지 않는다 */
    List<Item> findAllByStatusOrderByDisplayOrderAsc(ItemStatus status);
}
