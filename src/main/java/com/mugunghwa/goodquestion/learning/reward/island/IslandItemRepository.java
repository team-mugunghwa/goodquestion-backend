package com.mugunghwa.goodquestion.learning.reward.island;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IslandItemRepository extends JpaRepository<IslandItem, UUID> {

    @Query("""
            select ii from IslandItem ii
            join fetch ii.childItem ci join fetch ci.item
            where ii.island.id = :islandId
            """)
    List<IslandItem> findAllByIslandId(@Param("islandId") UUID islandId);

    Optional<IslandItem> findByChildItemId(UUID childItemId);

    int countByIslandId(UUID islandId);
}
