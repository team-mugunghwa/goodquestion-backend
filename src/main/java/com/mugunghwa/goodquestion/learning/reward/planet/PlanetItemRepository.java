package com.mugunghwa.goodquestion.learning.reward.planet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanetItemRepository extends JpaRepository<PlanetItem, UUID> {

    @Query("""
            select ii from PlanetItem ii
            join fetch ii.childItem ci join fetch ci.item
            where ii.planet.id = :planetId
            """)
    List<PlanetItem> findAllByPlanetId(@Param("planetId") UUID planetId);

    Optional<PlanetItem> findByChildItemId(UUID childItemId);

    int countByPlanetId(UUID planetId);
}
