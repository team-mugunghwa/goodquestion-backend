package com.mugunghwa.goodquestion.story.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StorySessionRepository extends JpaRepository<StorySession, UUID> {

    Optional<StorySession> findFirstByChildIdAndStatusOrderByLastActivityAtDesc(
            UUID childId, SessionStatus status);
    /** 이 아이가 이 이야기를 진행 중인 세션. 같은 이야기를 다시 시작하면 이걸 이어받는다. */
    Optional<StorySession> findFirstByChildIdAndStoryIdAndStatusOrderByLastActivityAtDesc(
            UUID childId, UUID storyId, SessionStatus status);
}
