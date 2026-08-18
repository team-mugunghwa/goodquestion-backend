package com.mugunghwa.goodquestion.story.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StorySessionRepository extends JpaRepository<StorySession, UUID> {

    Optional<StorySession> findFirstByChildIdAndStatusOrderByLastActivityAtDesc(
            UUID childId, SessionStatus status);
    /** 이 아이가 이 이야기를 진행 중인 세션. 같은 이야기를 다시 시작하면 이걸 이어받는다. */
    Optional<StorySession> findFirstByChildIdAndStoryIdAndStatusOrderByLastActivityAtDesc(
            UUID childId, UUID storyId, SessionStatus status);

    /** 이 아이가 이 이야기를 완주한 적이 있는가. 자유 대화의 진입 조건이다. */
    boolean existsByChildIdAndStoryIdAndStatus(UUID childId, UUID storyId, SessionStatus status);

    @Query("""
            select count(distinct s.story.id) from StorySession s
            where s.child.id = :childId and s.status = :status
            """)
    long countDistinctStories(@Param("childId") UUID childId,
                              @Param("status") SessionStatus status);
}
