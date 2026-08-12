package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.story.content.StoryScene;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 분석 LLM에 넘길 장면 값 (발화 분석 문서 4장).
 *
 * <p>LLM 호출은 트랜잭션 밖에서 일어나므로 그때는 {@link StoryScene}이 detached다. 필요한 값만
 * 트랜잭션 안에서 미리 떠 두는 것이 이 record의 역할이다.
 */
public record SceneAnalysisContext(String sceneContext, String goal,
                                   List<String> targetElements,
                                   Map<String, String> elementCriteria) {

    public static SceneAnalysisContext from(StoryScene scene) {
        return new SceneAnalysisContext(
                joinNonBlank(scene.getSceneDescription(), scene.getConflict()),
                scene.getSceneGoal(),
                scene.getRequiredElements(),
                scene.getElementCriteria());
    }

    private static String joinNonBlank(String... parts) {
        return Stream.of(parts)
                .filter(part -> part != null && !part.isBlank())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
    }
}
