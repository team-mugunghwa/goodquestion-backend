package com.mugunghwa.goodquestion.story.scene;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorySceneRepository extends JpaRepository<StoryScene, UUID> {

    List<StoryScene> findAllByStoryIdOrderBySceneOrderAsc(UUID storyId);

    Optional<StoryScene> findByStoryIdAndSceneOrder(UUID storyId, short sceneOrder);

    int countByStoryId(UUID storyId);

    Optional<StoryScene> findFirstByStoryIdOrderBySceneOrderAsc(UUID storyId);
}
