package com.mugunghwa.goodquestion.story.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorySessionRepository extends JpaRepository<StorySession, UUID> {

    Optional<StorySession> findFirstByChildIdAndStatusOrderByLastActivityAtDesc(
            UUID childId, SessionStatus status);
    /** 이 아이가 이 이야기를 진행 중인 세션. 같은 이야기를 다시 시작하면 이걸 이어받는다. */
    Optional<StorySession> findFirstByChildIdAndStoryIdAndStatusOrderByLastActivityAtDesc(
            UUID childId, UUID storyId, SessionStatus status);

    @Query("""
            select count(distinct s.story.id) from StorySession s
            where s.child.id = :childId and s.status = :status
            """)
    long countDistinctStories(@Param("childId") UUID childId,
                              @Param("status") SessionStatus status);

    /**
     * 6각 그래프 "지난 회차 평균"용 — 같은 아이의 다른 완료 세션 중 가장 최근 3건.
     * → claude/보호자리포트_6축그래프_설계안_D6.md 4장.
     */
    List<StorySession> findTop3ByChildIdAndStatusAndIdNotOrderByCompletedAtDesc(
            UUID childId, SessionStatus status, UUID excludedSessionId);
}
