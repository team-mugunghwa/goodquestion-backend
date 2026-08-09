package com.mugunghwa.goodquestion.story.content;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StoryRepository extends JpaRepository<Story, UUID> {

    List<Story> findAllByStatusOrderByCreatedAtDesc(StoryStatus status);

    /** 주제 필터링 — 명세가 주제 이름을 쿼리로 받으므로 id가 아닌 name으로 조인한다. */
    @Query("""
            select st.story from StoryTopic st
            where st.topic.name = :topicName and st.story.status = :status
            order by st.story.createdAt desc
            """)
    List<Story> findAllByTopicNameAndStatus(@Param("topicName") String topicName,
                                            @Param("status") StoryStatus status);

    List<Story> findTop3ByStatusOrderByCreatedAtDesc(StoryStatus status);
}
