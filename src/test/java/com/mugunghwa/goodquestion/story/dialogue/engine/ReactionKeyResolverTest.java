package com.mugunghwa.goodquestion.story.dialogue.engine;

import com.mugunghwa.goodquestion.global.vocab.ChildIntent;
import com.mugunghwa.goodquestion.global.vocab.ReactionKey;
import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.global.vocab.UtteranceValidity;
import com.mugunghwa.goodquestion.story.dialogue.DetectedElement;
import com.mugunghwa.goodquestion.story.dialogue.UtteranceAnalysis;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** 발화 성격 -> 반응 원칙 매핑의 단위 테스트 (대화 작동 규칙 3.1). */
class ReactionKeyResolverTest {

    private final ReactionKeyResolver resolver = new ReactionKeyResolver();

    private UtteranceAnalysis analysis(ChildIntent intent, UtteranceValidity validity,
                                       ThinkingElement... detected) {
        return UtteranceAnalysis.builder()
                .childIntent(intent)
                .detectedElements(java.util.Arrays.stream(detected)
                        .map(type -> new DetectedElement(type, "근거"))
                        .toList())
                .utteranceValidity(validity)
                .build();
    }

    @Test
    void 종료_턴은_의도와_무관하게_직접_반응이다() {
        UtteranceAnalysis playful = analysis(ChildIntent.PLAYFUL, UtteranceValidity.PLAYFUL);

        assertThat(resolver.resolve(playful, true)).isEqualTo(ReactionKey.DIRECT_RESPONSE);
    }

    @Test
    void 장난이나_주제_이탈은_장난받기다() {
        assertThat(resolver.resolve(analysis(ChildIntent.PLAYFUL, UtteranceValidity.PLAYFUL), false))
                .isEqualTo(ReactionKey.PLAYFUL_UTTERANCE);
        assertThat(resolver.resolve(analysis(ChildIntent.OFF_TOPIC, UtteranceValidity.OFF_TOPIC), false))
                .isEqualTo(ReactionKey.PLAYFUL_UTTERANCE);
    }

    @Test
    void 질문에는_먼저_답한다() {
        assertThat(resolver.resolve(analysis(ChildIntent.QUESTION, UtteranceValidity.VALID), false))
                .isEqualTo(ReactionKey.QUESTION_FROM_CHILD);
    }

    /** 캐릭터-05: 해결 제안은 짧은 발화보다 우선한다. */
    @Test
    void 짧아도_해결_요소가_잡혔으면_제안_인정이_우선이다() {
        UtteranceAnalysis shortProposal = analysis(
                ChildIntent.SHORT_RESPONSE, UtteranceValidity.SHORT, ThinkingElement.SOLUTION);

        assertThat(resolver.resolve(shortProposal, false)).isEqualTo(ReactionKey.PROPOSAL_FROM_CHILD);
    }

    @Test
    void 짧거나_불명확하면_짧게_되묻는다() {
        assertThat(resolver.resolve(analysis(ChildIntent.SHORT_RESPONSE, UtteranceValidity.SHORT), false))
                .isEqualTo(ReactionKey.UNCLEAR_UTTERANCE);
        assertThat(resolver.resolve(analysis(ChildIntent.UNCLEAR, UtteranceValidity.UNCLEAR), false))
                .isEqualTo(ReactionKey.UNCLEAR_UTTERANCE);
    }

    @Test
    void 공감_요소가_잡히면_공감_반응이다() {
        UtteranceAnalysis empathy = analysis(
                ChildIntent.EMOTION, UtteranceValidity.VALID, ThinkingElement.EMPATHY);

        assertThat(resolver.resolve(empathy, false)).isEqualTo(ReactionKey.EMPATHY_FROM_CHILD);
    }

    @Test
    void 의견과_반박과_결정은_이견_반응이다() {
        for (ChildIntent intent : List.of(ChildIntent.OPINION, ChildIntent.REASONING,
                ChildIntent.DECISION, ChildIntent.PERSPECTIVE, ChildIntent.REQUEST,
                ChildIntent.CHALLENGE)) {
            assertThat(resolver.resolve(analysis(intent, UtteranceValidity.VALID), false))
                    .as("intent=%s", intent)
                    .isEqualTo(ReactionKey.DISAGREEMENT);
        }
    }

    @Test
    void 감정_표현은_직접_반응이다() {
        assertThat(resolver.resolve(analysis(ChildIntent.EMOTION, UtteranceValidity.VALID), false))
                .isEqualTo(ReactionKey.DIRECT_RESPONSE);
    }

    /** 진행-14: 약한 유도 생략 대상은 장난 / 질문 / 불명확 반응이다. */
    @Test
    void 약한_유도_허용_여부는_반응_키로_가른다() {
        assertThat(resolver.allowsSoftCue(ReactionKey.PLAYFUL_UTTERANCE)).isFalse();
        assertThat(resolver.allowsSoftCue(ReactionKey.QUESTION_FROM_CHILD)).isFalse();
        assertThat(resolver.allowsSoftCue(ReactionKey.UNCLEAR_UTTERANCE)).isFalse();
        assertThat(resolver.allowsSoftCue(ReactionKey.PROPOSAL_FROM_CHILD)).isTrue();
        assertThat(resolver.allowsSoftCue(ReactionKey.DISAGREEMENT)).isTrue();
        assertThat(resolver.allowsSoftCue(ReactionKey.EMPATHY_FROM_CHILD)).isTrue();
        assertThat(resolver.allowsSoftCue(ReactionKey.DIRECT_RESPONSE)).isTrue();
    }
}
