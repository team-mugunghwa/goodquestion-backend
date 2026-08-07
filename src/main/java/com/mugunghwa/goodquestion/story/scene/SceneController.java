package com.mugunghwa.goodquestion.story.scene;

import com.mugunghwa.goodquestion.story.scene.dto.SceneContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stories/{storyId}/scenes")
@RequiredArgsConstructor
public class SceneController {

    private final StorySceneRepository sceneRepository;

    /** 이야기 시작 전 장면 콘텐츠 프리페치 — 도입·전개 재생과 화면 구성을 위해 사용 */
    @GetMapping
    public List<SceneContentResponse> getScenes(@PathVariable UUID storyId) {
        // TODO: 이야기 PUBLISHED 검증
        return sceneRepository.findAllByStoryIdOrderBySceneOrderAsc(storyId).stream()
                .map(SceneContentResponse::from).toList();
    }
}
