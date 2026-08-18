package com.mugunghwa.goodquestion.story.freetalk;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FreeTalkMessageRepository extends JpaRepository<FreeTalkMessage, UUID> {

    /** LLM에 넘길 대화 이력. 순서가 곧 문맥이라 turn_order로 정렬한다. */
    List<FreeTalkMessage> findAllByFreeTalkIdOrderByTurnOrderAsc(UUID freeTalkId);

    /**
     * 다음 순번. 겹친 요청이 같은 번호를 잡으면 유니크 제약이 잡아 준다 -
     * messages(session_id, turn_order)와 같은 방식이다.
     */
    @Query("""
            select coalesce(max(m.turnOrder), -1) + 1
            from FreeTalkMessage m
            where m.freeTalk.id = :freeTalkId
            """)
    short nextTurnOrder(@Param("freeTalkId") UUID freeTalkId);

    /** 이미 닫힌 대화의 마무리 대사 - 다시 만들지 않고 남아 있는 것을 돌려준다. */
    Optional<FreeTalkMessage> findFirstByFreeTalkIdAndRoleOrderByTurnOrderDesc(
            UUID freeTalkId, FreeTalkRole role);
}
