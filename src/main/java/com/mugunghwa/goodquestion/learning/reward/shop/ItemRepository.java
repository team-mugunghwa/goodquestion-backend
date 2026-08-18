package com.mugunghwa.goodquestion.learning.reward.shop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    List<Item> findAllByOrderByDisplayOrderAsc();

    /**
     * 운영 중 내린 아이템(HIDDEN)은 상점에 노출하지 않는다.
     *
     * <p>해금 이야기를 페치 조인으로 함께 읽는다. LAZY로 두면 두 가지가 생긴다 -
     * 아이템마다 이야기 조회가 따로 나가고(STORY_COMPLETE 4건), 무엇보다 호출자가
     * 트랜잭션 안에 있어야만 제목을 읽을 수 있다. 캐시 적재는 트랜잭션 없이
     * 부르고 싶으므로(ItemCatalog 주석) 여기서 다 읽어 둔다.
     */
    @Query("""
            select i from Item i
            left join fetch i.unlockStory
            where i.status = :status
            order by i.displayOrder asc
            """)
    List<Item> findAllActiveWithUnlockStory(@Param("status") ItemStatus status);
}
