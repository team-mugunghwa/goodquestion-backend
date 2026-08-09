package com.mugunghwa.goodquestion.learning.reward.planet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlanetRepository extends JpaRepository<Planet, UUID> {

    Optional<Planet> findByChildId(UUID childId);
}
