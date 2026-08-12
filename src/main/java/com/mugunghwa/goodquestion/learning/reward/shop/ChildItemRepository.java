package com.mugunghwa.goodquestion.learning.reward.shop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChildItemRepository extends JpaRepository<ChildItem, UUID> {

    @Query("select ci from ChildItem ci join fetch ci.item where ci.child.id = :childId order by ci.acquiredAt")
    List<ChildItem> findAllByChildId(@Param("childId") UUID childId);

    /** 보유 검증 — 남의 아이 아이템을 배치하지 못하게 한다 */
    Optional<ChildItem> findByIdAndChildId(UUID id, UUID childId);
}
