package com.mugunghwa.goodquestion.story.dialogue.engine;

import com.mugunghwa.goodquestion.global.vocab.ResponseMode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.story.content.SceneType;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.session.SceneEndReason;
import com.mugunghwa.goodquestion.story.session.StorySession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 진행 판단 규칙의 단위 테스트.
 *
 * <p>LLM도 DB도 타지 않는 순수 규칙이라 스프링 컨텍스트 없이 검증한다.
 * 이 판단이 틀리면 장면이 안 끝나거나, 아이가 말을 꺼내기도 전에 유도가 들어간다.
 */
class ProgressionEngineTest {

    private final ProgressionEngine engine = new ProgressionEngine(new GuidanceTargetSelector());

    /** 필수 요소 2개, 최소 2턴, 최대 4턴짜리 대화 장면. */
    private StoryScene scene() {
        return StoryScene.builder()
                .sceneType(SceneType.DIALOGUE)
                .requiredElements(List.of("SOLUTION", "REASON"))
                .preferredTurns((short) 2)
                .maxTurns((short) 4)
                .build();
    }

    private StorySession session() {
        return StorySession.builder().build();
    }

    @Test
    void 필수_요소를_다_채우고_최소_대화량을_넘기면_목표_달성으로_끝낸다() {
        StorySession session = session();
        session.applyTurn(List.of("SOLUTION"), false);
        session.applyTurn(List.of("REASON"), false);

        ProgressionDecision decision = engine.decide(session, scene(), ResponseMode.NORMAL);

        assertThat(decision.mode()).isEqualTo(ResponseMode.CLOSING);
        assertThat(decision.closingReason()).isEqualTo(SceneEndReason.GOAL_MET);
    }

    @Test
    void 요소를_다_채워도_최소_대화량_전이면_끝내지_않는다() {
        StorySession session = session();
        session.applyTurn(List.of("SOLUTION", "REASON"), false);

        ProgressionDecision decision = engine.decide(session, scene(), null);

        assertThat(decision.mode()).isEqualTo(ResponseMode.NORMAL);
    }

    @Test
    void 요소가_남아도_최대_턴에_닿으면_끝낸다() {
        StorySession session = session();
        for (int i = 0; i < 4; i++) {
            session.applyTurn(List.of(), false);
        }

        ProgressionDecision decision = engine.decide(session, scene(), ResponseMode.NORMAL);

        assertThat(decision.mode()).isEqualTo(ResponseMode.CLOSING);
        assertThat(decision.closingReason()).isEqualTo(SceneEndReason.MAX_TURNS);
    }

    @Test
    void 첫_발화에는_유도하지_않는다() {
        StorySession session = session();
        session.applyTurn(List.of(), false);

        assertThat(engine.decide(session, scene(), null).mode()).isEqualTo(ResponseMode.NORMAL);
    }

    @Test
    void 신규_요소_없이_두_턴이_지나면_유도한다() {
        StorySession session = session();
        session.applyTurn(List.of(), false);
        session.applyTurn(List.of(), false);

        ProgressionDecision decision = engine.decide(session, scene(), ResponseMode.NORMAL);

        assertThat(decision.mode()).isEqualTo(ResponseMode.GUIDED);
        assertThat(decision.guidanceTarget()).isEqualTo(ThinkingElement.SOLUTION);
    }

    @Test
    void 직전_턴이_유도였으면_연달아_유도하지_않는다() {
        StorySession session = session();
        session.applyTurn(List.of(), false);
        session.applyTurn(List.of(), false);

        ProgressionDecision decision = engine.decide(session, scene(), ResponseMode.GUIDED);

        assertThat(decision.mode()).isEqualTo(ResponseMode.NORMAL);
    }

    @Test
    void 이번_턴에_새_요소가_나왔으면_유도하지_않는다() {
        StorySession session = session();
        session.applyTurn(List.of(), false);
        session.applyTurn(List.of(), false);
        session.applyTurn(List.of("SOLUTION"), false);

        assertThat(engine.decide(session, scene(), ResponseMode.NORMAL).mode())
                .isEqualTo(ResponseMode.NORMAL);
    }

    @Test
    void 저정보_발화가_두_번_이어지면_유도한다() {
        StorySession session = session();
        session.applyTurn(List.of("SOLUTION"), true);
        session.applyTurn(List.of("SOLUTION"), true);

        ProgressionDecision decision = engine.decide(session, scene(), ResponseMode.NORMAL);

        // SOLUTION은 이미 누적됐으므로 남은 REASON을 유도한다.
        assertThat(decision.mode()).isEqualTo(ResponseMode.GUIDED);
        assertThat(decision.guidanceTarget()).isEqualTo(ThinkingElement.REASON);
    }

    @Test
    void 남은_턴이_채울_요소보다_많지_않으면_유도한다() {
        // 최대 4턴 중 2턴을 썼고 요소는 2개가 남았다 - 이제부터 한 턴도 흘릴 수 없다.
        StorySession session = session();
        session.applyTurn(List.of(), false);
        session.applyTurn(List.of(), false);

        assertThat(engine.decide(session, scene(), ResponseMode.NORMAL).mode())
                .isEqualTo(ResponseMode.GUIDED);
    }
}
