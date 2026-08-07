package com.mugunghwa.goodquestion.story.story;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StoryRepository extends JpaRepository<Story, UUID> {

    Page<Story> findAllByStatus(StoryStatus status, Pageable pageable);

    /** 토픽 필터링 — story_topics 조인 */
    @Query("""
            select st.story from StoryTopic st
            where st.topic.id = :topicId and st.story.status = :status
            """)
    Page<Story> findAllByTopicAndStatus(@Param("topicId") UUID topicId,
                                        @Param("status") StoryStatus status,
                                        Pageable pageable);

    List<Story> findTop3ByStatusOrderByCreatedAtDesc(StoryStatus status);
}
