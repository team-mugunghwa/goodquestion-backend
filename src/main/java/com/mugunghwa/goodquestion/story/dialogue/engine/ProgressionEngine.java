package com.mugunghwa.goodquestion.story.dialogue.engine;

import com.mugunghwa.goodquestion.global.vocab.ResponseMode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.session.SceneEndReason;
import com.mugunghwa.goodquestion.story.session.StorySession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 진행 규칙 엔진 (발화 분석 문서 10~11장) — LLM 미사용, 순수 규칙.
 * 인프라 의존 없이 단위 테스트 가능하게 유지할 것.
 *
 * <p>호출 시점이 중요하다. {@link StorySession#applyTurn}으로 이번 턴이 반영된 뒤에 부른다 -
 * 종료 판정이 "이번 발화까지 포함한" 누적 요소와 턴 수를 봐야 하기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class ProgressionEngine {

    /** 신규 요소 없이 이어진 턴이 이만큼 쌓이면 유도한다(진행-10). */
    private static final int STALLED_TURNS_THRESHOLD = 2;
    /** 저정보 발화가 이만큼 이어지면 유도한다(진행-15). */
    private static final int LOW_INFORMATION_TURNS_THRESHOLD = 2;
    /** 대화 필드가 비어 있는 장면을 만났을 때의 방어값. DB check상 DIALOGUE에는 항상 값이 있다. */
    private static final int DEFAULT_MAX_TURNS = 5;

    private final GuidanceTargetSelector guidanceTargetSelector;

    /**
     * 판단 순서 (문서 11장):
     * 1. 종료 조건 — (필수 요소 충족 && 최소 대화량[preferred_turns] 충족) → CLOSING(GOAL_MET)
     *               / 최대 대화 범위[max_turns] 도달 → CLOSING(MAX_TURNS)
     * 2. 강한 유도 제한 — 첫 발화 / 이번 턴 새 요소 확인됨 / 직전 턴이 GUIDED → GUIDED 금지
     * 3. 유도 필요성 — 필수 요소 잔여 && (turns_without_new_element >= 2
     *               || consecutive_low_information_turns >= 2 || 남은 대화 기회 부족) → GUIDED
     * 4. 그 외 → NORMAL
     *
     * @param previousMode 직전 턴의 모드. 이번 턴이 반영되면서 세션의 값이 덮이므로
     *                     호출부가 applyTurn 전에 읽어 넘긴다
     */
    public ProgressionDecision decide(StorySession session, StoryScene scene,
                                      ResponseMode previousMode) {
        List<ThinkingElement> missing = scene.missingElements(session.getAccumulatedElements());
        int turnCount = session.getCurrentChildTurnCount();

        // 1. 종료 판단이 가장 먼저다. 끝낼 턴에 유도를 붙이면 마무리 대사와 겹친다.
        if (missing.isEmpty() && turnCount >= preferredTurns(scene)) {
            return ProgressionDecision.closing(SceneEndReason.GOAL_MET);
        }
        if (turnCount >= maxTurns(scene)) {
            return ProgressionDecision.closing(SceneEndReason.MAX_TURNS);
        }

        // 2 + 3. 유도는 "해도 되는가"와 "해야 하는가"를 모두 만족할 때만 한다.
        if (!missing.isEmpty() && isGuidanceAllowed(session, turnCount, previousMode)
                && isGuidanceNeeded(session, scene, turnCount, missing.size())) {
            ThinkingElement target = guidanceTargetSelector.select(session, scene);
            if (target != null) {
                return ProgressionDecision.guided(target);
            }
        }

        return ProgressionDecision.normal();
    }

    /**
     * 강한 유도 제한(진행-11). 셋 중 하나라도 걸리면 유도하지 않는다.
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

    /** 유도 필요성(진행-10, 15). 대화가 멈췄거나, 남은 턴으로는 목표를 못 채울 때. */
    private boolean isGuidanceNeeded(StorySession session, StoryScene scene,
                                     int turnCount, int missingCount) {
        boolean stalled = session.getTurnsWithoutNewElement() >= STALLED_TURNS_THRESHOLD;
        boolean lowInformation =
                session.getConsecutiveLowInformationTurns() >= LOW_INFORMATION_TURNS_THRESHOLD;
        // 남은 턴이 채울 요소 수보다 많지 않으면 이제부터는 한 턴도 흘려보낼 수 없다.
        boolean runningOutOfTurns = (maxTurns(scene) - turnCount) <= missingCount;

        return stalled || lowInformation || runningOutOfTurns;
    }

    private int preferredTurns(StoryScene scene) {
        return scene.getPreferredTurns() != null ? scene.getPreferredTurns() : 1;
    }

    private int maxTurns(StoryScene scene) {
        return scene.getMaxTurns() != null ? scene.getMaxTurns() : DEFAULT_MAX_TURNS;
    }
}
