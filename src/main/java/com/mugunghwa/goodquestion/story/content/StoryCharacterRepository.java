package com.mugunghwa.goodquestion.story.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoryCharacterRepository extends JpaRepository<StoryCharacter, UUID> {

    List<StoryCharacter> findAllByStoryId(UUID storyId);

    Optional<StoryCharacter> findByStoryIdAndCharacterKey(UUID storyId, String characterKey);
}
