package com.mugunghwa.goodquestion.story.session;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /** 이 아이가 이 이야기를 완주한 적이 있는가. 자유 대화의 진입 조건이다. */
    boolean existsByChildIdAndStoryIdAndStatus(UUID childId, UUID storyId, SessionStatus status);

    @Query("""
            select count(distinct s.story.id) from StorySession s
            where s.child.id = :childId and s.status = :status
            """)
    long countDistinctStories(@Param("childId") UUID childId,
                              @Param("status") SessionStatus status);

    /**
     * 이 아이가 완주한 이야기의 id들. 홈의 완주 개수({@link #countDistinctStories})와
     * <b>같은 근거를 쓴다</b> - 개수와 목록이 다른 표를 보면 "3편 끝냈다"는데 도장은
     * 두 개만 찍히는 화면이 나온다.
     *
     * <p>완주 횟수표(child_story_play_counts)를 쓰지 않는 것도 자유 대화의 진입
     * 판정({@link #existsByChildIdAndStoryIdAndStatus})과 같은 이유다 - 저 표는 보상
     * 지급이 걸려 있어 보상 규칙이 바뀌면 함께 흔들린다.
     */
    @Query("""
            select distinct s.story.id from StorySession s
            where s.child.id = :childId and s.status = :status
            """)
    List<UUID> findDistinctStoryIds(@Param("childId") UUID childId,
                                    @Param("status") SessionStatus status);

    /**
     * 6각 그래프 "지난 회차 평균"용 — 같은 아이의 다른 완료 세션 중 가장 최근 3건.
     * → claude/보호자리포트_6축그래프_설계안_D6.md 4장.
     */
    List<StorySession> findTop3ByChildIdAndStatusAndIdNotOrderByCompletedAtDesc(
            UUID childId, SessionStatus status, UUID excludedSessionId);
}
