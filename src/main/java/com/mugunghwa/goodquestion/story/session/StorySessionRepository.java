package com.mugunghwa.goodquestion.story.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StorySessionRepository extends JpaRepository<StorySession, UUID> {

    Optional<StorySession> findFirstByChildIdAndStatusOrderByLastActivityAtDesc(
            UUID childId, SessionStatus status);
}
