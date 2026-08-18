package com.mugunghwa.goodquestion.story.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoryCharacterRepository extends JpaRepository<StoryCharacter, UUID> {

    List<StoryCharacter> findAllByStoryId(UUID storyId);

    /**
     * 화면에 줄 세울 순서까지 정해서 준다. 시드가 한 문장으로 들어가면 created_at이 같아지므로
     * character_key를 뒤에 붙여 순서가 조회마다 흔들리지 않게 한다.
     */
    List<StoryCharacter> findAllByStoryIdOrderByCreatedAtAscCharacterKeyAsc(UUID storyId);

    Optional<StoryCharacter> findByStoryIdAndCharacterKey(UUID storyId, String characterKey);
}
