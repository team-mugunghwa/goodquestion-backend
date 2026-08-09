package com.mugunghwa.goodquestion.story.dialogue.engine;

import com.mugunghwa.goodquestion.global.vocab.ResponseMode;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.story.session.SceneEndReason;

/** 진행 판단 결과 */
public record ProgressionDecision(
        ResponseMode mode,
        ThinkingElement guidanceTarget,   // GUIDED일 때만
        SceneEndReason closingReason      // CLOSING일 때만
) {
    public static ProgressionDecision normal() {
        return new ProgressionDecision(ResponseMode.NORMAL, null, null);
    }

    public static ProgressionDecision guided(ThinkingElement target) {
        return new ProgressionDecision(ResponseMode.GUIDED, target, null);
    }

    public static ProgressionDecision closing(SceneEndReason reason) {
        return new ProgressionDecision(ResponseMode.CLOSING, null, reason);
    }
}
