package com.mugunghwa.goodquestion.story.content;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.story.content.dto.SceneContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SceneService {

    private final StorySceneRepository sceneRepository;
    private final SceneAudioResolver sceneAudioResolver;

    /** getScenes의 PUBLISHED 검증을 위해 추가 */
    private final StoryRepository storyRepository;


    /**
     * 장면 콘텐츠 프리페치 (클라이언트용).
     * 컨트롤러가 리포지토리를 직접 호출하던 것을 서비스로 옮겼다.
     * PUBLISHED 검증이 필요한데, 검증은 컨트롤러가 아니라 서비스의 책임이기 때문.
     *
     * 비공개 이야기는 존재 사실도 노출하지 않도록 FORBIDDEN이 아니라 NOT_FOUND로 응답한다.
     * SceneContentResponse는 element_criteria·remaining_worries·미션 노출 조건을 제외한다.
     */
    public List<SceneContentResponse> getScenes(UUID storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이야기를 찾을 수 없습니다."));
        if (story.getStatus() != StoryStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "이야기를 찾을 수 없습니다.");
        }
        return sceneRepository.findAllByStoryIdOrderBySceneOrderAsc(storyId).stream()
                .map(this::toContent)
                .toList();
    }



    public StoryScene getFirstScene(UUID storyId) {
        return sceneRepository.findFirstByStoryIdOrderBySceneOrderAsc(storyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "장면이 없습니다."));
    }

    /** 다음 장면. 마지막 장면이면 empty → 후속 활동 전환 신호 */
    public Optional<StoryScene> getNextScene(StoryScene current) {
        return sceneRepository.findByStoryIdAndSceneOrder(
                current.getStory().getId(), (short) (current.getSceneOrder() + 1));
    }

    /**
     * 장면 콘텐츠에 사전 렌더 내레이션을 붙인다.
     *
     * <p>DIALOGUE 장면은 내레이션이 없고, STORY 장면도 음성이 아직 없으면 null이 나가
     * 클라이언트가 지금처럼 음성 없이 진행한다.
     */
    private SceneContentResponse toContent(StoryScene scene) {
        return SceneContentResponse.from(scene, scene.isDialogue() ? null
                : sceneAudioResolver.narrationOf(scene.getId(), scene.getSceneDescription()).orElse(null));
    }
}
