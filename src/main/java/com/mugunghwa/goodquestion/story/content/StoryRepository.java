package com.mugunghwa.goodquestion.story.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StoryRepository extends JpaRepository<Story, UUID> {

    /**
     * 노출 순서는 display_order 오름차순이 먼저다. created_at은 순서를 정하지 않은
     * 이야기들 사이의 갈림길로만 남는다 - 시드가 created_at을 적지 않아 한 마이그레이션에서
     * 들어간 이야기끼리는 값이 같고, 그때 순서는 실행 계획에 따라 달라진다.
     */
    List<Story> findAllByStatusOrderByDisplayOrderAscCreatedAtDesc(StoryStatus status);

    /** 주제 필터링 — 명세가 주제 이름을 쿼리로 받으므로 id가 아닌 name으로 조인한다. */
    @Query("""
            select st.story from StoryTopic st
            where st.topic.name = :topicName and st.story.status = :status
            order by st.story.displayOrder asc, st.story.createdAt desc
            """)
    List<Story> findAllByTopicNameAndStatus(@Param("topicName") String topicName,
                                            @Param("status") StoryStatus status);

    List<Story> findTop3ByStatusOrderByDisplayOrderAscCreatedAtDesc(StoryStatus status);
}
