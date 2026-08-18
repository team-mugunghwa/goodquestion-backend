package com.mugunghwa.goodquestion.story.freetalk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface FreeTalkRepository extends JpaRepository<FreeTalk, UUID> {

    /**
     * 인물 고르기 화면의 "마지막으로 이야기한 때". 인물별 최근 1건만 필요하므로
     * 대화 전체를 읽어 오지 않고 집계로 끝낸다.
     */
    @Query("""
            select new com.mugunghwa.goodquestion.story.freetalk.LastTalk(
                       f.character.id, max(f.createdAt))
            from FreeTalk f
            where f.child.id = :childId and f.story.id = :storyId
            group by f.character.id
            """)
    List<LastTalk> findLastTalkedAt(@Param("childId") UUID childId,
                                    @Param("storyId") UUID storyId);

    /**
     * 턴을 하나 올린다. <b>읽은 값과 지금 값이 같을 때만</b> 올라간다.
     *
     * <p>턴 준비와 저장 사이에 LLM 왕복이 있어 그 틈에 다른 요청이 끼어들 수 있다.
     * 엔티티를 읽어 필드를 고치는 방식으로는 둘 다 성공해 턴이 두 번 올라가고 상한이
     * 새는데, 조건부 갱신은 진 쪽이 0행을 받아 스스로 물러난다.
     *
     * @return 1이면 이 요청이 그 턴의 주인, 0이면 이미 누가 올렸거나 대화가 닫혔다
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update FreeTalk f set f.turnCount = :next
            where f.id = :id and f.turnCount = :expected and f.endedAt is null
            """)
    int advanceTurn(@Param("id") UUID id, @Param("expected") short expected,
                    @Param("next") short next);

    /** 마지막 턴 - 턴을 올리면서 같은 문장으로 대화를 닫는다. 둘이 갈리면 상한이 샌다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update FreeTalk f set f.turnCount = :next, f.endedAt = :endedAt
            where f.id = :id and f.turnCount = :expected and f.endedAt is null
            """)
    int advanceTurnAndEnd(@Param("id") UUID id, @Param("expected") short expected,
                          @Param("next") short next,
                          @Param("endedAt") OffsetDateTime endedAt);

    /**
     * 열려 있으면 닫는다.
     *
     * @return 1이면 이 요청이 대화를 닫았고 마무리 대사를 남길 자격이 있다,
     *         0이면 그사이 누가 먼저 닫았다 - 그쪽 대사를 그대로 쓴다
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update FreeTalk f set f.endedAt = :endedAt where f.id = :id and f.endedAt is null")
    int endIfOpen(@Param("id") UUID id, @Param("endedAt") OffsetDateTime endedAt);
}
