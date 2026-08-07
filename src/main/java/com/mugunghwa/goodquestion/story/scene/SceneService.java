package com.mugunghwa.goodquestion.story.scene;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SceneService {

    private final StorySceneRepository sceneRepository;

    public StoryScene getFirstScene(UUID storyId) {
        // TODO: sceneOrder 최솟값 장면 반환
        throw new UnsupportedOperationException("TODO");
    }

    /** 다음 장면. 마지막 장면이면 empty → 후속 활동 전환 신호 */
    public Optional<StoryScene> getNextScene(StoryScene current) {
        return sceneRepository.findByStoryIdAndSceneOrder(
                current.getStory().getId(), (short) (current.getSceneOrder() + 1));
    }
}
