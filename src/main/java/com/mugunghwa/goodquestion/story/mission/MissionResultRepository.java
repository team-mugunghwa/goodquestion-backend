package com.mugunghwa.goodquestion.story.mission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MissionResultRepository extends JpaRepository<MissionResult, UUID> {

    Optional<MissionResult> findBySessionIdAndMissionId(UUID sessionId, String missionId);

    boolean existsBySessionIdAndMissionId(UUID sessionId, String missionId);

    List<MissionResult> findAllBySessionId(UUID sessionId);
}
