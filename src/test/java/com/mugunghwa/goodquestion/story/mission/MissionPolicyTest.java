package com.mugunghwa.goodquestion.story.mission;

import com.mugunghwa.goodquestion.global.vocab.ChildIntent;
import com.mugunghwa.goodquestion.global.vocab.UtteranceValidity;
import com.mugunghwa.goodquestion.story.content.SceneType;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.dialogue.UtteranceAnalysis;
import com.mugunghwa.goodquestion.story.session.StorySession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 미션 노출 판단의 단위 테스트.
 *
 * <p>너무 일찍 띄우면 정답 찾기가 되고, 너무 늦게 띄우면 장면이 끝나도록 뜨지 않는다.
 */
class MissionPolicyTest {

    private final MissionPolicy policy = new MissionPolicy(new MissionConfigReader());

    private StoryScene scene(MissionType type) {
        return StoryScene.builder()
                .sceneType(SceneType.DIALOGUE)
                .requiredElements(List.of("SOLUTION", "REASON"))
                .missionConfig(Map.of("mission_id", "mission_1", "mission_type", type.name()))
                .build();
    }

    private UtteranceAnalysis analysis(UtteranceValidity validity) {
        return UtteranceAnalysis.builder()
                .childIntent(ChildIntent.OPINION)
                .detectedElements(List.of())
                .utteranceValidity(validity)
                .build();
    }

    @Test
    void 해결_방향이_나온_뒤_문제해결_미션을_노출한다() {
        StorySession session = StorySession.builder().build();
        session.applyTurn(List.of("SOLUTION"), false);

        assertThat(policy.shouldExpose(session, scene(MissionType.PROBLEM_SOLVING),
                analysis(UtteranceValidity.VALID))).isTrue();
    }

    @Test
    void 아직_아무_말도_하지_않았다면_노출하지_않는다() {
        StorySession session = StorySession.builder().build();

        assertThat(policy.shouldExpose(session, scene(MissionType.PROBLEM_SOLVING),
                analysis(UtteranceValidity.VALID))).isFalse();
    }

    @Test
    void 저정보_발화에는_노출하지_않는다() {
        StorySession session = StorySession.builder().build();
        session.applyTurn(List.of(), true);

        assertThat(policy.shouldExpose(session, scene(MissionType.PROBLEM_SOLVING),
                analysis(UtteranceValidity.SHORT))).isFalse();
    }

    @Test
    void 필수_요소를_이미_다_채웠다면_노출하지_않는다() {
        StorySession session = StorySession.builder().build();
        session.applyTurn(List.of("SOLUTION", "REASON"), false);

        assertThat(policy.shouldExpose(session, scene(MissionType.PROBLEM_SOLVING),
                analysis(UtteranceValidity.VALID))).isFalse();
    }

    @Test
    void 해결_방향이_없는_첫_발화에는_노출하지_않는다() {
        StorySession session = StorySession.builder().build();
        session.applyTurn(List.of("EMOTION"), false);

        assertThat(policy.shouldExpose(session, scene(MissionType.PROBLEM_SOLVING),
                analysis(UtteranceValidity.VALID))).isFalse();
    }

    @Test
    void 두_번_대화해도_실행_방법이_없으면_문제해결_미션을_노출한다() {
        StorySession session = StorySession.builder().build();
        session.applyTurn(List.of("EMOTION"), false);
        session.applyTurn(List.of(), false);

        assertThat(policy.shouldExpose(session, scene(MissionType.PROBLEM_SOLVING),
                analysis(UtteranceValidity.VALID))).isTrue();
    }

    @Test
    void 관점전환_미션은_관점이_나온_뒤에도_두_번_대화를_기다린다() {
        StoryScene scene = scene(MissionType.PERSPECTIVE_SHIFT);
        StorySession session = StorySession.builder().build();

        session.applyTurn(List.of("PERSPECTIVE"), false);
        assertThat(policy.shouldExpose(session, scene, analysis(UtteranceValidity.VALID))).isFalse();

        session.applyTurn(List.of(), false);
        assertThat(policy.shouldExpose(session, scene, analysis(UtteranceValidity.VALID))).isTrue();
    }

    @Test
    void 관점이_나오지_않으면_두_번_대화해도_관점전환_미션을_노출하지_않는다() {
        StoryScene scene = scene(MissionType.PERSPECTIVE_SHIFT);
        StorySession session = StorySession.builder().build();
        session.applyTurn(List.of("EMOTION"), false);
        session.applyTurn(List.of("REASON"), false);

        assertThat(policy.shouldExpose(session, scene, analysis(UtteranceValidity.VALID))).isFalse();
    }
}
