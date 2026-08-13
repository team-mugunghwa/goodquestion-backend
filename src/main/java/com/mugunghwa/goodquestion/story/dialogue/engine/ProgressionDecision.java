package com.mugunghwa.goodquestion.story.dialogue.engine;

import com.mugunghwa.goodquestion.global.vocab.ResponseMode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.story.session.SceneEndReason;

/**
 * 진행 판단 결과.
 *
 * @param guidanceTarget GUIDED(강한 유도) 또는 soft-cue(약한 유도)의 대상 요소.
 *                       세션의 last_guidance_target에 그대로 기록된다 — 약한 유도도
 *                       기록해야 다음 턴의 "직전 유도 요소 반복 금지"가 성립한다
 * @param softCue        NORMAL 모드에서 캐릭터의 남은 걱정을 가볍게 얹는 약한 유도(진행-13).
 *                       responseMode는 여전히 NORMAL이라 장면 보너스 자격을 잃지 않는다
 */
public record ProgressionDecision(
        ResponseMode mode,
        ThinkingElement guidanceTarget,
        SceneEndReason closingReason,     // CLOSING일 때만
        boolean softCue
) {
    public static ProgressionDecision normal() {
        return new ProgressionDecision(ResponseMode.NORMAL, null, null, false);
    }

    /** 약한 유도(soft-cue) — 모드는 NORMAL, 걱정 정보만 부가로 전달한다. */
    public static ProgressionDecision softCue(ThinkingElement target) {
        return new ProgressionDecision(ResponseMode.NORMAL, target, null, true);
    }

    public static ProgressionDecision guided(ThinkingElement target) {
        return new ProgressionDecision(ResponseMode.GUIDED, target, null, false);
    }

    public static ProgressionDecision closing(SceneEndReason reason) {
        return new ProgressionDecision(ResponseMode.CLOSING, null, reason, false);
    }
}
