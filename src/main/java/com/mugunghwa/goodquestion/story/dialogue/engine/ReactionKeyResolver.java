package com.mugunghwa.goodquestion.story.dialogue.engine;

import com.mugunghwa.goodquestion.global.vocab.ChildIntent;
import com.mugunghwa.goodquestion.global.vocab.ReactionKey;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.global.vocab.UtteranceValidity;
import com.mugunghwa.goodquestion.story.dialogue.DetectedElement;
import com.mugunghwa.goodquestion.story.dialogue.UtteranceAnalysis;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 발화 성격 -> 반응 원칙 키 매핑 (대화 작동 규칙 3.1) — LLM 미사용 규칙.
 *
 * <p>판정 순서가 곧 우선순위다. 표의 순서를 따르되 문서가 명시한 예외 하나를 지킨다 —
 * 해결 제안은 짧은 발화보다 우선한다(캐릭터-05). 아이가 짧게라도 제안을 했으면
 * 되묻지 않고 제안으로 받는다.
 */
@Component
public class ReactionKeyResolver {

    private static final Set<ChildIntent> DISAGREEMENT_INTENTS = Set.of(
            ChildIntent.OPINION, ChildIntent.REASONING, ChildIntent.DECISION,
            ChildIntent.PERSPECTIVE, ChildIntent.REQUEST, ChildIntent.CHALLENGE);

    /**
     * @param closing 종료 턴이면 의도 매핑을 무시하고 마무리 톤의 직접 반응을 쓴다(캐릭터-13).
     *                중간 턴의 "장면을 닫는 톤 금지" 지시와 충돌하지 않게 하기 위해서다.
     */
    public ReactionKey resolve(UtteranceAnalysis analysis, boolean closing) {
        if (closing) {
            return ReactionKey.DIRECT_RESPONSE;
        }
        ChildIntent intent = analysis.getChildIntent();
        UtteranceValidity validity = analysis.getUtteranceValidity();

        if (validity == UtteranceValidity.PLAYFUL || validity == UtteranceValidity.OFF_TOPIC
                || intent == ChildIntent.PLAYFUL || intent == ChildIntent.OFF_TOPIC) {
            return ReactionKey.PLAYFUL_UTTERANCE;
        }
        if (intent == ChildIntent.QUESTION) {
            return ReactionKey.QUESTION_FROM_CHILD;
        }
        if (intent == ChildIntent.SOLUTION || detected(analysis, ThinkingElement.SOLUTION)) {
            return ReactionKey.PROPOSAL_FROM_CHILD;
        }
        if (validity == UtteranceValidity.SHORT || validity == UtteranceValidity.UNCLEAR
                || intent == ChildIntent.SHORT_RESPONSE || intent == ChildIntent.UNCLEAR) {
            return ReactionKey.UNCLEAR_UTTERANCE;
        }
        if (detected(analysis, ThinkingElement.EMPATHY)) {
            return ReactionKey.EMPATHY_FROM_CHILD;
        }
        if (DISAGREEMENT_INTENTS.contains(intent)) {
            return ReactionKey.DISAGREEMENT;
        }
        return ReactionKey.DIRECT_RESPONSE;
    }

    /** 약한 유도를 생략할 반응(진행-14): 장난 / 질문 / 불명확. */
    public boolean allowsSoftCue(ReactionKey key) {
        return key != ReactionKey.PLAYFUL_UTTERANCE
                && key != ReactionKey.QUESTION_FROM_CHILD
                && key != ReactionKey.UNCLEAR_UTTERANCE;
    }

    private boolean detected(UtteranceAnalysis analysis, ThinkingElement element) {
        if (analysis.getDetectedElements() == null) {
            return false;
        }
        return analysis.getDetectedElements().stream()
                .map(DetectedElement::type)
                .anyMatch(type -> type == element);
    }
}
