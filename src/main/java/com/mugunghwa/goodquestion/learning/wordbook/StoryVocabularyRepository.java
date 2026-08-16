package com.mugunghwa.goodquestion.learning.wordbook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StoryVocabularyRepository extends JpaRepository<StoryVocabulary, UUID> {

    Optional<StoryVocabulary> findByStoryIdAndWord(UUID storyId, String word);
}
