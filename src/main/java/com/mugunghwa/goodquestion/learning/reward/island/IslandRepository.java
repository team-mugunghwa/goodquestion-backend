package com.mugunghwa.goodquestion.learning.reward.island;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IslandRepository extends JpaRepository<Island, UUID> {

    Optional<Island> findByChildId(UUID childId);
}
