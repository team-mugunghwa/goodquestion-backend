package com.mugunghwa.goodquestion.learning.reward.planet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PlanetItemRepository extends JpaRepository<PlanetItem, UUID> {

    @Query("""
            select ii from PlanetItem ii
            join fetch ii.childItem ci join fetch ci.item
            where ii.planet.id = :planetId
            """)
    List<PlanetItem> findAllByPlanetId(@Param("planetId") UUID planetId);

    Optional<PlanetItem> findByChildItemId(UUID childItemId);

    /** 놓기·옮기기 전 빈 칸인지 확인. DB 유니크는 동시 요청용 최후 방어다 */
    boolean existsByPlanetIdAndPlacedQAndPlacedR(UUID planetId, short placedQ, short placedR);

    boolean existsByChildItemId(UUID childItemId);

    /** 보관함 계산용 - 보유 아이템마다 조회하면 목록 크기만큼 질의가 붙는다 */
    @Query("select pi.childItem.id from PlanetItem pi where pi.childItem.child.id = :childId")
    Set<UUID> findPlacedChildItemIds(@Param("childId") UUID childId);

    int countByPlanetId(UUID planetId);
}
