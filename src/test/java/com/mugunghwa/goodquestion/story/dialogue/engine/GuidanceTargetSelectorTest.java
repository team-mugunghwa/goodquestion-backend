package com.mugunghwa.goodquestion.story.dialogue.engine;

import com.mugunghwa.goodquestion.global.vocab.ResponseMode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.story.content.SceneType;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.session.StorySession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GuidanceTargetSelectorTest {

    private final GuidanceTargetSelector selector = new GuidanceTargetSelector();

    private StoryScene scene() {
        return StoryScene.builder()
                .sceneType(SceneType.DIALOGUE)
                .requiredElements(List.of("SOLUTION", "REASON", "RESULT"))
                .build();
    }

    @Test
    void 미충족_요소를_장면_선언_순서대로_고른다() {
        assertThat(selector.select(StorySession.builder().build(), scene()))
                .isEqualTo(ThinkingElement.SOLUTION);
    }

    @Test
    void 이미_확인된_요소는_건너뛴다() {
        StorySession session = StorySession.builder().build();
        session.applyTurn(List.of("SOLUTION"), false);

        assertThat(selector.select(session, scene())).isEqualTo(ThinkingElement.REASON);
    }

    @Test
    void 직전에_유도한_요소는_다시_고르지_않는다() {
        StorySession session = StorySession.builder().build();
        session.recordDecision(ResponseMode.GUIDED, ThinkingElement.SOLUTION);

        assertThat(selector.select(session, scene())).isEqualTo(ThinkingElement.REASON);
    }

    @Test
    void 남은_선택지가_직전_요소뿐이면_같은_요소로_돌아간다() {
        StorySession session = StorySession.builder().build();
        session.applyTurn(List.of("REASON", "RESULT"), false);
        session.recordDecision(ResponseMode.GUIDED, ThinkingElement.SOLUTION);

        assertThat(selector.select(session, scene())).isEqualTo(ThinkingElement.SOLUTION);
    }

    @Test
    void 미충족_요소가_없으면_null이다() {
        StorySession session = StorySession.builder().build();
        session.applyTurn(List.of("SOLUTION", "REASON", "RESULT"), false);

        assertThat(selector.select(session, scene())).isNull();
    }
}
