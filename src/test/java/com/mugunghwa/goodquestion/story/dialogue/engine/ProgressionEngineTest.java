package com.mugunghwa.goodquestion.story.dialogue.engine;

import com.mugunghwa.goodquestion.global.vocab.ChildIntent;
import com.mugunghwa.goodquestion.global.vocab.ResponseMode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.global.vocab.UtteranceValidity;
import com.mugunghwa.goodquestion.story.content.SceneType;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.dialogue.UtteranceAnalysis;
import com.mugunghwa.goodquestion.story.session.SceneEndReason;
import com.mugunghwa.goodquestion.story.session.StorySession;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 진행 판단 규칙의 단위 테스트 (대화 작동 규칙 2.2, 발화 분석 문서 11장).
 *
 * <p>LLM도 DB도 타지 않는 순수 규칙이라 스프링 컨텍스트 없이 검증한다.
 * 이 판단이 틀리면 장면이 안 끝나거나, 아이가 말을 꺼내기도 전에 유도가 들어간다.
 */
class ProgressionEngineTest {

    private final ProgressionEngine engine = new ProgressionEngine(
            new GuidanceTargetSelector(), new ReactionKeyResolver(),
            new ProgressionProperties(2, 2, 2));

    /** 필수 요소 2개, 최소 2턴, 최대 5턴짜리 대화 장면. */
    private StoryScene scene() {
        return scene((short) 5);
    }

    private StoryScene scene(short maxTurns) {
        return StoryScene.builder()
                .sceneType(SceneType.DIALOGUE)
                .requiredElements(List.of("SOLUTION", "REASON"))
                .preferredTurns((short) 2)
                .maxTurns(maxTurns)
                .build();
    }

    private StorySession session() {
        return StorySession.builder().build();
    }

    /** 반응 원칙이 soft-cue를 허용하지 않는 발화(불명확) — soft-cue와 무관한 검증에 쓴다. */
    private UtteranceAnalysis unclear() {
        return UtteranceAnalysis.builder()
                .childIntent(ChildIntent.UNCLEAR)
                .detectedElements(List.of())
                .utteranceValidity(UtteranceValidity.UNCLEAR)
                .build();
    }

    /** soft-cue를 허용하는 유효 발화(의견). */
    private UtteranceAnalysis opinion() {
        return UtteranceAnalysis.builder()
                .childIntent(ChildIntent.OPINION)
                .detectedElements(List.of())
                .utteranceValidity(UtteranceValidity.VALID)
                .build();
    }

    @Test
    void 필수_요소를_다_채우고_최소_대화량을_넘기면_목표_달성으로_끝낸다() {
        StorySession session = session();
        session.applyTurn(List.of("SOLUTION"), false);
        session.applyTurn(List.of("REASON"), false);

        ProgressionDecision decision = engine.decide(session, scene(), ResponseMode.NORMAL, opinion());

        assertThat(decision.mode()).isEqualTo(ResponseMode.CLOSING);
        assertThat(decision.closingReason()).isEqualTo(SceneEndReason.GOAL_MET);
    }

    @Test
    void 요소를_다_채워도_최소_대화량_전이면_끝내지_않는다() {
        StorySession session = session();
        session.applyTurn(List.of("SOLUTION", "REASON"), false);

        ProgressionDecision decision = engine.decide(session, scene(), null, opinion());

        assertThat(decision.mode()).isEqualTo(ResponseMode.NORMAL);
    }

    @Test
    void 요소가_남아도_최대_턴에_닿으면_끝낸다() {
        StorySession session = session();
        for (int i = 0; i < 5; i++) {
            session.applyTurn(List.of(), false);
        }

        ProgressionDecision decision = engine.decide(session, scene(), ResponseMode.NORMAL, unclear());

        assertThat(decision.mode()).isEqualTo(ResponseMode.CLOSING);
        assertThat(decision.closingReason()).isEqualTo(SceneEndReason.MAX_TURNS);
    }

    /**
     * CLOSING 결정에는 유도 대상이 실리지 않는다. 이 불변식이 깨지면 끝내는 턴에
     * [강한 유도]의 물음-종결 지시가 걸려 마무리 대사와 물음이 겹친다 —
     * 프롬프트 쪽 CLOSING 가드(CharacterPromptBuilder.userPrompt)와 짝을 이루는 방어다.
     */
    @Test
    void 끝내는_턴에는_유도_대상을_싣지_않는다() {
        StorySession session = session();
        for (int i = 0; i < 5; i++) {
            session.applyTurn(List.of(), false);
        }

        ProgressionDecision decision = engine.decide(session, scene(), ResponseMode.NORMAL, unclear());

        assertThat(decision.mode()).isEqualTo(ResponseMode.CLOSING);
        assertThat(decision.guidanceTarget()).isNull();
        assertThat(decision.softCue()).isFalse();
    }

    /**
     * 두 종료 조건이 같이 걸리면 목표 달성이 우선한다. 요소를 다 채운 턴은 최대 턴에
     * 닿아도 MAX_TURNS 판정에 이르지 못하므로, 미션 보류(TurnTransactions)가
     * 마지막 턴을 따로 처리해야 한다 - 그 전제를 여기서 못박는다.
     */
    @Test
    void 요소를_다_채운_채_최대_턴에_닿으면_목표_달성_종료가_우선한다() {
        StorySession session = session();
        for (int i = 0; i < 5; i++) {
            session.applyTurn(List.of("SOLUTION", "REASON"), false);
        }

        ProgressionDecision decision = engine.decide(session, scene(), ResponseMode.NORMAL, opinion());

        assertThat(decision.mode()).isEqualTo(ResponseMode.CLOSING);
        assertThat(decision.closingReason()).isEqualTo(SceneEndReason.GOAL_MET);
    }

    @Test
    void 첫_발화에는_유도하지_않는다() {
        StorySession session = session();
        session.applyTurn(List.of(), false);

        assertThat(engine.decide(session, scene(), null, unclear()).mode())
                .isEqualTo(ResponseMode.NORMAL);
    }

    @Test
    void 신규_요소_없이_두_턴이_지나면_유도한다() {
        StorySession session = session();
        session.applyTurn(List.of(), false);
        session.applyTurn(List.of(), false);

        ProgressionDecision decision = engine.decide(session, scene(), ResponseMode.NORMAL, unclear());

        assertThat(decision.mode()).isEqualTo(ResponseMode.GUIDED);
        assertThat(decision.guidanceTarget()).isEqualTo(ThinkingElement.SOLUTION);
        assertThat(decision.softCue()).isFalse();
    }

    @Test
    void 직전_턴이_유도였으면_연달아_유도하지_않는다() {
        StorySession session = session();
        session.applyTurn(List.of(), false);
        session.applyTurn(List.of(), false);

        ProgressionDecision decision = engine.decide(session, scene(), ResponseMode.GUIDED, unclear());

        assertThat(decision.mode()).isEqualTo(ResponseMode.NORMAL);
    }

    @Test
    void 이번_턴에_새_요소가_나왔으면_강하게_유도하지_않는다() {
        StorySession session = session();
        session.applyTurn(List.of(), false);
        session.applyTurn(List.of(), false);
        session.applyTurn(List.of("SOLUTION"), false);

        assertThat(engine.decide(session, scene(), ResponseMode.NORMAL, opinion()).mode())
                .isEqualTo(ResponseMode.NORMAL);
    }

    @Test
    void 저정보_발화가_두_번_이어지면_유도한다() {
        StorySession session = session();
        session.applyTurn(List.of("SOLUTION"), true);
        session.applyTurn(List.of("SOLUTION"), true);

        ProgressionDecision decision = engine.decide(session, scene(), ResponseMode.NORMAL, unclear());

        // SOLUTION은 이미 누적됐으므로 남은 REASON을 유도한다.
        assertThat(decision.mode()).isEqualTo(ResponseMode.GUIDED);
        assertThat(decision.guidanceTarget()).isEqualTo(ThinkingElement.REASON);
    }

    /** 남은 턴 기준은 미충족 요소 수가 아니라 고정 임계값 2다 (대화 작동 규칙 2.2 확정). */
    @Test
    void 남은_턴이_2_이하면_유도한다() {
        StorySession session = session();
        session.applyTurn(List.of(), false);
        session.applyTurn(List.of(), true);
        session.applyTurn(List.of(), false);   // 3턴 종료, 남은 턴 = 5 - 3 = 2

        assertThat(engine.decide(session, scene(), ResponseMode.NORMAL, unclear()).mode())
                .isEqualTo(ResponseMode.GUIDED);
    }

    @Test
    void 남은_턴이_3_이상이면_그_이유만으로는_유도하지_않는다() {
        StorySession session = session();
        session.applyTurn(List.of("SOLUTION"), false);
        session.applyTurn(List.of(), false);   // 2턴 종료, 남은 턴 = 3, 무진전 1, 저정보 0

        assertThat(engine.decide(session, scene(), ResponseMode.NORMAL, unclear()).mode())
                .isEqualTo(ResponseMode.NORMAL);
    }

    // ----- 약한 유도 (soft-cue, 진행-13/14) -----

    @Test
    void 신규_요소가_나왔지만_필수_요소가_남았으면_약한_유도를_얹는다() {
        StorySession session = session();
        session.applyTurn(List.of("SOLUTION"), false);   // 신규 요소, REASON 잔여

        ProgressionDecision decision = engine.decide(session, scene(), null, opinion());

        assertThat(decision.mode()).isEqualTo(ResponseMode.NORMAL);
        assertThat(decision.softCue()).isTrue();
        assertThat(decision.guidanceTarget()).isEqualTo(ThinkingElement.REASON);
    }

    @Test
    void 장난_질문_불명확_반응에는_약한_유도를_생략한다() {
        StorySession session = session();
        session.applyTurn(List.of("SOLUTION"), false);   // 신규 요소, REASON 잔여

        UtteranceAnalysis playful = UtteranceAnalysis.builder()
                .childIntent(ChildIntent.PLAYFUL)
                .detectedElements(List.of())
                .utteranceValidity(UtteranceValidity.PLAYFUL)
                .build();
        ProgressionDecision decision = engine.decide(session, scene(), null, playful);

        assertThat(decision.mode()).isEqualTo(ResponseMode.NORMAL);
        assertThat(decision.softCue()).isFalse();
        assertThat(decision.guidanceTarget()).isNull();
    }

    @Test
    void 필수_요소를_다_채운_턴에는_약한_유도가_없다() {
        StorySession session = session();
        session.applyTurn(List.of("SOLUTION", "REASON"), false);   // missing 없음, 최소 턴 전

        ProgressionDecision decision = engine.decide(session, scene(), null, opinion());

        assertThat(decision.mode()).isEqualTo(ResponseMode.NORMAL);
        assertThat(decision.softCue()).isFalse();
    }

    @Test
    void 진전_없는_턴에는_약한_유도를_쓰지_않는다() {
        StorySession session = session();
        session.applyTurn(List.of("SOLUTION"), false);
        session.applyTurn(List.of(), false);   // 무진전 1턴 - 강한 유도 조건도 미달

        ProgressionDecision decision = engine.decide(session, scene(), ResponseMode.NORMAL, opinion());

        assertThat(decision.mode()).isEqualTo(ResponseMode.NORMAL);
        assertThat(decision.softCue()).isFalse();
    }
}
