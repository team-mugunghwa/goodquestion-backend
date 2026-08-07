package com.mugunghwa.goodquestion.story.topic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StoryTopicRepository extends JpaRepository<StoryTopic, StoryTopic.Pk> {

    @Query("select st from StoryTopic st join fetch st.topic where st.story.id in :storyIds")
    List<StoryTopic> findAllByStoryIds(@Param("storyIds") List<UUID> storyIds);
}
