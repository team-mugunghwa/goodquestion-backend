package com.mugunghwa.goodquestion.story.session;

import com.mugunghwa.goodquestion.global.vocab.ResponseMode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 진행 판단이 참조하는 누적 상태의 단위 테스트.
 *
 * <p>연관 엔티티를 건드리지 않는 순수 상태 전이라 DB와 스프링 컨텍스트 없이 검증한다.
 * 이 값이 어긋나면 유도 시점과 장면 종료 판정이 통째로 틀어진다.
 */
class StorySessionTest {

    private StorySession newSession() {
        return StorySession.builder().build();
    }

    @Test
    void 새_요소는_누적되고_중복은_쌓이지_않는다() {
        StorySession session = newSession();

        session.applyTurn(List.of("REASON", "EMOTION"), false, ResponseMode.NORMAL, null);
        session.applyTurn(List.of("REASON", "SOLUTION"), false, ResponseMode.NORMAL, null);

        assertThat(session.getAccumulatedElements())
                .containsExactly("REASON", "EMOTION", "SOLUTION");
    }

    @Test
    void 턴마다_발화_횟수가_증가한다() {
        StorySession session = newSession();

        session.applyTurn(List.of("REASON"), false, ResponseMode.NORMAL, null);
        session.applyTurn(List.of(), false, ResponseMode.NORMAL, null);

        assertThat(session.getCurrentChildTurnCount()).isEqualTo((short) 2);
    }

    @Test
    void 신규_요소가_없으면_무진전_카운트가_오른다() {
        StorySession session = newSession();

        session.applyTurn(List.of(), false, ResponseMode.NORMAL, null);
        session.applyTurn(List.of(), false, ResponseMode.NORMAL, null);

        assertThat(session.getTurnsWithoutNewElement()).isEqualTo((short) 2);
    }

    @Test
    void 이미_누적된_요소만_다시_나오면_진전으로_보지_않는다() {
        StorySession session = newSession();

        session.applyTurn(List.of("REASON"), false, ResponseMode.NORMAL, null);
        session.applyTurn(List.of("REASON"), false, ResponseMode.NORMAL, null);

        assertThat(session.getTurnsWithoutNewElement()).isEqualTo((short) 1);
        assertThat(session.getAccumulatedElements()).containsExactly("REASON");
    }

    @Test
    void 신규_요소가_나오면_무진전_카운트가_초기화된다() {
        StorySession session = newSession();

        session.applyTurn(List.of(), false, ResponseMode.NORMAL, null);
        session.applyTurn(List.of(), false, ResponseMode.NORMAL, null);
        session.applyTurn(List.of("SOLUTION"), false, ResponseMode.NORMAL, null);

        assertThat(session.getTurnsWithoutNewElement()).isZero();
    }

    @Test
    void 저정보_발화가_이어지면_카운트가_오르고_유효_발화에서_초기화된다() {
        StorySession session = newSession();

        session.applyTurn(List.of(), true, ResponseMode.NORMAL, null);
        session.applyTurn(List.of(), true, ResponseMode.NORMAL, null);
        assertThat(session.getConsecutiveLowInformationTurns()).isEqualTo((short) 2);

        session.applyTurn(List.of("REASON"), false, ResponseMode.NORMAL, null);
        assertThat(session.getConsecutiveLowInformationTurns()).isZero();
    }

    @Test
    void 직전_모드와_유도_대상이_기록된다() {
        StorySession session = newSession();

        session.applyTurn(List.of(), false, ResponseMode.GUIDED, ThinkingElement.SOLUTION);

        assertThat(session.getLastResponseMode()).isEqualTo(ResponseMode.GUIDED);
        assertThat(session.getLastGuidanceTarget()).isEqualTo(ThinkingElement.SOLUTION);
        assertThat(session.getLastDetectedElements()).isEmpty();
    }

    @Test
    void 유도_모드가_한_번이라도_나오면_장면_보너스_자격을_잃는다() {
        StorySession session = newSession();

        session.applyTurn(List.of("REASON"), false, ResponseMode.GUIDED, ThinkingElement.SOLUTION);
        session.applyTurn(List.of("SOLUTION"), false, ResponseMode.NORMAL, null);
        session.closeScene(SceneEndReason.GOAL_MET, true);

        assertThat(session.isSceneBonusEligible()).isFalse();
    }

    @Test
    void 유도_없이_목표를_달성하면_장면_보너스_자격이_있다() {
        StorySession session = newSession();

        session.applyTurn(List.of("REASON", "SOLUTION"), false, ResponseMode.NORMAL, null);
        session.closeScene(SceneEndReason.GOAL_MET, true);

        assertThat(session.isSceneBonusEligible()).isTrue();
    }

    @Test
    void 장면을_이동하면_누적_상태가_초기화된다() {
        StorySession session = newSession();

        session.applyTurn(List.of("REASON"), true, ResponseMode.GUIDED, ThinkingElement.SOLUTION);
        session.moveToScene(null);

        assertThat(session.getCurrentChildTurnCount()).isZero();
        assertThat(session.getAccumulatedElements()).isEmpty();
        assertThat(session.getLastDetectedElements()).isEmpty();
        assertThat(session.getTurnsWithoutNewElement()).isZero();
        assertThat(session.getConsecutiveLowInformationTurns()).isZero();
        assertThat(session.isSceneBonusEligible()).isFalse();
    }
}