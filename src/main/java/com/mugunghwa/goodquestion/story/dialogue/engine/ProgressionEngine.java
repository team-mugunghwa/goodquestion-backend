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
     * 2. 강한 유도 제한 — 반응이 장난/질문/불명확(진행-14) / 첫 발화 / 이번 턴 새 요소 확인됨
     *    / 직전 턴이 GUIDED → GUIDED 금지
     * <p><b>2026-08-17 기본값 변경</b> - {@code progression.guidance.always-guide}가 켜져 있으면
     * 2·3의 제한을 건너뛰고 <b>필수 요소가 남는 한 매 턴 유도</b>한다(첫 발화 제외).
     * 원 규칙(진행-09/10)은 "무진전 2연속 등"이었는데, 실제로는 요소를 하나만 채워도 그 턴
     * 유도가 꺼지고 직전 유도 다음 턴도 꺼져서 4~5턴 장면에서 유도가 한두 번밖에 걸리지
     * 않았다. 성인 테스트와 팀 시연에서 "유도가 안 된다"는 지적이 반복돼 기본값을 바꿨다.
     * 설정을 false로 두면 문서의 원 규칙으로 돌아간다. 단 진행-14(장난/질문/불명확)는
     * always-guide와 무관하게 유지한다 — 유도 빈도의 문제가 아니라 아이 말을 받는 문제다.
     *
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
        //
        // 최소 대화량 게이트는 원 자료의 요건이다 - 발화_분석_및_진행_판단_연동_기준.md
        // 10.3("최소한의 대화 과정이 진행되고 필수 사고 요소가 충족됨")과 11절 판단 순서
        // ("필수 요소 충족 및 최소 대화량 충족")가 둘의 AND를 종료 조건으로 명시한다.
        // 한때 충족조건 문서를 근거로 게이트를 제거했다가(#68 커밋 8) 원 자료 요건 우선
        // 원칙에 따라 복원했다(2026-08-16). 수치(전 장면 2)만 운영 위임분이다.
        if (missing.isEmpty() && turnCount >= preferredTurns(scene)) {
            return ProgressionDecision.closing(SceneEndReason.GOAL_MET);
        }
        if (turnCount >= maxTurns(scene)) {
            return ProgressionDecision.closing(SceneEndReason.MAX_TURNS);
        }

        // 2 + 3. 강한 유도는 "해도 되는가"와 "해야 하는가"를 모두 만족할 때만 한다.
        if (!missing.isEmpty() && !reactionBlocksGuidance(session, scene, turnCount, analysis)
                && isGuidanceAllowed(session, turnCount, previousMode)
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
     * 반응에 따른 유도 보류(진행-14). 장난 / 질문 / 불명확에는 걱정을 들이밀지 않는다 —
     * 아이가 물어봤는데 캐릭터가 제 걱정부터 꺼내면 아이 말이 통째로 무시된다.
     *
     * <p>이 규칙은 원래 약한 유도 분기에만 걸려 있었다. always-guide가 켜지면서 강한 유도가
     * 그 분기보다 앞에서 매 턴 반환되어 규칙이 조용히 꺼졌다(2026-08-18 발견).
     *
     * <p>진행-14를 강한 유도까지 넓히면 진행-10(저정보 2연속이면 유도)과 정면으로 부딪친다 -
     * 저정보 발화는 대개 불명확으로 찍히기 때문이다. 둘의 우선순위를 여기서 정한다:
     * <b>진행-10의 필요 신호(무진전/저정보/남은 턴)가 서 있으면 보류를 풀고 유도한다.</b>
     * 진행-14가 막는 것은 지금이 아니어도 되는데 걱정부터 들이미는 턴이다.
     * 이 조정이 없으면 짧게만 답하는 아이에게 유도가 영영 걸리지 않는다.
     */
    private boolean reactionBlocksGuidance(StorySession session, StoryScene scene,
                                           int turnCount, UtteranceAnalysis analysis) {
        ReactionKey key = reactionKeyResolver.resolve(analysis, false);
        if (reactionKeyResolver.allowsSoftCue(key)) {
            return false;
        }
        return !documentedGuidanceNeeded(session, scene, turnCount);
    }

    /**
     * 강한 유도 제한(진행-09). 셋 중 하나라도 걸리면 유도하지 않는다.
     *
     * <p>말문이 트이기 전과, 아이가 스스로 진전을 만든 직후와, 방금 유도한 직후에
     * 또 밀어붙이면 대화가 아니라 추궁이 된다.
     */
    private boolean isGuidanceAllowed(StorySession session, int turnCount, ResponseMode previousMode) {
        boolean firstUtterance = turnCount <= 1;
        if (firstUtterance) {
            // 첫 발화는 아이가 스스로 꺼내는 자리다. 여기까지 밀면 추궁이 된다.
            return false;
        }
        if (properties.guidesAlways()) {
            // 강한 유도 기본 모드: 요소가 남아 있는 한 매 턴 이끈다.
            return true;
        }
        boolean newElementThisTurn = session.getTurnsWithoutNewElement() == 0;
        boolean guidedLastTurn = previousMode == ResponseMode.GUIDED;

        return !newElementThisTurn && !guidedLastTurn;
    }

    /**
     * 유도 필요성(진행-10). 대화가 멈췄거나, 장면 종료까지 남은 기회가 적을 때.
     *
     * <p>남은 턴 기준은 미충족 요소 수가 아니라 고정 임계값이다(대화 작동 규칙 2.2 확정:
     * "남은 턴 <= 2"). 요소 수와 비교하면 요소가 많은 장면에서 유도가 너무 이르게 걸린다.
     */
    private boolean isGuidanceNeeded(StorySession session, StoryScene scene, int turnCount) {
        if (properties.guidesAlways()) {
            // 필요성 판정도 생략한다 - 호출부가 이미 "필수 요소가 남았는가"를 확인했다.
            return true;
        }
        return documentedGuidanceNeeded(session, scene, turnCount);
    }

    /** 진행-10의 원 신호. always-guide가 켜져도 진행-14와의 우선순위 판정에는 이 값을 쓴다. */
    private boolean documentedGuidanceNeeded(StorySession session, StoryScene scene, int turnCount) {
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
