package com.mugunghwa.goodquestion.story.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StorySessionRepository extends JpaRepository<StorySession, UUID> {

    /**
     * 소유권 검증과 장면 확인까지 한 번에 끝내는 조회.
     *
     * <p>{@code findById}로 읽으면 세션 1회 + 소유권 검증이 보는 child 1회 +
     * 호출자가 보는 currentScene 1회로 세 번이 나간다. 둘 다 사실상 항상 필요하므로
     * 함께 읽는다. 턴 하나가 트랜잭션 셋으로 쪼개져 있어(TurnTransactions) 이 조회가
     * 턴당 세 번 반복되고, 1차 캐시가 트랜잭션 경계를 넘지 못해 매번 다시 나간다.
     *
     * <p><b>left join이어야 한다.</b> current_scene_id는 nullable이라 inner join으로
     * 쓰면 장면이 없는 세션이 결과에서 사라져 "세션을 찾을 수 없습니다"가 된다.
     * 둘 다 @ManyToOne이라 행이 늘어나는 문제는 없다(컬렉션 페치 조인과 다르다).
     */
    @Query("""
            select s from StorySession s
            left join fetch s.child
            left join fetch s.currentScene
            where s.id = :id
            """)
    Optional<StorySession> findByIdWithChildAndScene(@Param("id") UUID id);

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
}
