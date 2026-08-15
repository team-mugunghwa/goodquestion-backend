package com.mugunghwa.goodquestion.story.dialogue.engine;

import com.mugunghwa.goodquestion.global.vocab.ReactionKey;
import com.mugunghwa.goodquestion.global.vocab.ResponseMode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.dialogue.UtteranceAnalysis;
import com.mugunghwa.goodquestion.story.session.SceneEndReason;
import com.mugunghwa.goodquestion.story.session.StorySession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 진행 규칙 엔진 (발화 분석 문서 10~11장, 대화 작동 규칙 2.2) — LLM 미사용, 순수 규칙.
 * 인프라 의존 없이 단위 테스트 가능하게 유지할 것.
 *
 * <p>호출 시점이 중요하다. {@link StorySession#applyTurn}으로 이번 턴이 반영된 뒤에 부른다 -
 * 종료 판정이 "이번 발화까지 포함한" 누적 요소와 턴 수를 봐야 하기 때문이다.
 *
 * <p>임계값(2/2/2)은 {@link ProgressionProperties}로 조정한다(진행-17).
 */
@Component
@RequiredArgsConstructor
public class ProgressionEngine {

    /** 대화 필드가 비어 있는 장면을 만났을 때의 방어값. DB check상 DIALOGUE에는 항상 값이 있다. */
    private static final int DEFAULT_MAX_TURNS = 5;

    private final GuidanceTargetSelector guidanceTargetSelector;
    private final ReactionKeyResolver reactionKeyResolver;
    private final ProgressionProperties properties;

    /**
     * 판단 순서 (대화 작동 규칙 2.2, 발화 분석 문서 11장):
     * 1. 종료 — (필수 요소 충족 && 최소 대화량[preferred_turns] 충족) → CLOSING(GOAL_MET)
     *          / 최대 대화 범위[max_turns] 도달 → CLOSING(MAX_TURNS)
     * 2. 강한 유도 제한 — 첫 발화 / 이번 턴 새 요소 확인됨 / 직전 턴이 GUIDED → GUIDED 금지
     * 3. 유도 필요성 — 필수 요소 잔여 && (무진전 2연속 || 저정보 2연속 || 남은 턴 <= 2) → GUIDED
     * 4. 약한 유도(soft-cue, 진행-13) — NORMAL이지만 이번 턴 신규 요소 확인 && 필수 요소 잔여
     *    && 반응이 장난/질문/불명확이 아니면 걱정을 가볍게 얹는다
     * 5. 그 외 → NORMAL
     *
     * @param previousMode 직전 턴의 모드. 이번 턴이 반영되면서 세션의 값이 덮이므로
     *                     호출부가 applyTurn 전에 읽어 넘긴다
     */
    public ProgressionDecision decide(StorySession session, StoryScene scene,
                                      ResponseMode previousMode, UtteranceAnalysis analysis) {
        List<ThinkingElement> missing = scene.missingElements(session.getAccumulatedElements());
        int turnCount = session.getCurrentChildTurnCount();

        // 1. 종료 판단이 가장 먼저다. 끝낼 턴에 유도를 붙이면 마무리 대사와 겹친다.
        // preferred_turns는 최소 턴이 아니라 권장 길이다(대화N_충족조건.md 확정).
        // 게이트로 쓰면 1턴에 요소를 다 채워도 한 턴을 더 돌아야 하는데, 그 턴에는
        // 캐릭터가 물을 것이 없다 — 남은 요소가 없어 유도 대상 선정이 빈다(08-15 요청 #9-3).
        if (missing.isEmpty()) {
            return ProgressionDecision.closing(SceneEndReason.GOAL_MET);
        }
        if (turnCount >= maxTurns(scene)) {
            return ProgressionDecision.closing(SceneEndReason.MAX_TURNS);
        }

        // 2 + 3. 강한 유도는 "해도 되는가"와 "해야 하는가"를 모두 만족할 때만 한다.
        if (!missing.isEmpty() && isGuidanceAllowed(session, turnCount, previousMode)
                && isGuidanceNeeded(session, scene, turnCount)) {
            ThinkingElement target = guidanceTargetSelector.select(session, scene);
            if (target != null) {
                return ProgressionDecision.guided(target);
            }
        }

        // 4. 약한 유도 — 진전이 있어 강한 유도는 막혔지만 아직 채울 요소가 남은 턴.
        //    반응 중심에 걱정을 부가로 얹는다. 장난/질문/불명확 반응이면 생략한다(진행-14).
        if (!missing.isEmpty() && session.getTurnsWithoutNewElement() == 0
                && reactionKeyResolver.allowsSoftCue(reactionKeyResolver.resolve(analysis, false))) {
            ThinkingElement softTarget = guidanceTargetSelector.select(session, scene);
            if (softTarget != null) {
                return ProgressionDecision.softCue(softTarget);
            }
        }

        return ProgressionDecision.normal();
    }

    /**
     * 강한 유도 제한(진행-09). 셋 중 하나라도 걸리면 유도하지 않는다.
     *
     * <p>말문이 트이기 전과, 아이가 스스로 진전을 만든 직후와, 방금 유도한 직후에
     * 또 밀어붙이면 대화가 아니라 추궁이 된다.
     */
    private boolean isGuidanceAllowed(StorySession session, int turnCount, ResponseMode previousMode) {
        boolean firstUtterance = turnCount <= 1;
        boolean newElementThisTurn = session.getTurnsWithoutNewElement() == 0;
        boolean guidedLastTurn = previousMode == ResponseMode.GUIDED;

        return !firstUtterance && !newElementThisTurn && !guidedLastTurn;
    }

    /**
     * 유도 필요성(진행-10). 대화가 멈췄거나, 장면 종료까지 남은 기회가 적을 때.
     *
     * <p>남은 턴 기준은 미충족 요소 수가 아니라 고정 임계값이다(대화 작동 규칙 2.2 확정:
     * "남은 턴 <= 2"). 요소 수와 비교하면 요소가 많은 장면에서 유도가 너무 이르게 걸린다.
     */
    private boolean isGuidanceNeeded(StorySession session, StoryScene scene, int turnCount) {
        boolean stalled = session.getTurnsWithoutNewElement() >= properties.stalledTurns();
        boolean lowInformation =
                session.getConsecutiveLowInformationTurns() >= properties.lowInformationTurns();
        boolean runningOutOfTurns = (maxTurns(scene) - turnCount) <= properties.remainingTurns();

        return stalled || lowInformation || runningOutOfTurns;
    }

    private int preferredTurns(StoryScene scene) {
        return scene.getPreferredTurns() != null ? scene.getPreferredTurns() : 1;
    }

    private int maxTurns(StoryScene scene) {
        return scene.getMaxTurns() != null ? scene.getMaxTurns() : DEFAULT_MAX_TURNS;
    }
}
