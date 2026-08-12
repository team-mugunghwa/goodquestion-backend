package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.ai.character.CharacterLlmClient;
import com.mugunghwa.goodquestion.global.vocab.CharacterEmotion;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.dialogue.engine.ProgressionDecision;
import com.mugunghwa.goodquestion.story.session.StorySession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * 캐릭터 응답 (NORMAL/GUIDED).
 * 전달 정보: 아이 최신 발화, 분석 결과, 진행 모드, 캐릭터 감정·상황,
 * (유도 시) scene.getRemainingWorry(guidanceTarget) — 요소별 캐릭터의 남은 걱정.
 * 원칙: 직접적 학습 질문 금지 — 캐릭터의 상황·감정 안에서 걱정을 드러낸다.
 *
 * <p>무엇을 말할지는 이미 정해진 상태로 들어온다. 여기서 하는 일은 결정된 모드와 유도 대상을
 * 캐릭터 프롬프트 입력으로 옮기고, 돌아온 감정 문자열을 아는 값으로 맞추는 것뿐이다 -
 * 판정을 LLM에 넘기지 않는다는 원칙(정책-02)이 이 경계에서 지켜진다.
 */
@Service
@RequiredArgsConstructor
public class CharacterResponseService {

    private final CharacterLlmClient characterLlmClient;

    public CharacterReply reply(StorySession session, StoryScene scene,
                                UtteranceAnalysis analysis, ProgressionDecision decision) {
        CharacterLlmClient.CharacterLlmResult result = characterLlmClient.reply(
                new CharacterLlmClient.CharacterLlmInput(
                        analysis.getMessage().getText(),
                        summarize(analysis),
                        decision.mode().name(),
                        characterContext(scene),
                        remainingWorry(scene, decision)));

        return new CharacterReply(session.personalize(result.text()), toEmotion(result.emotion()));
    }

    /**
     * 유도 시에는 해당 요소의 남은 걱정을 넘긴다. 일반 대화에서도 남은 걱정을 함께 넘겨
     * 캐릭터가 약하게 흘릴 수 있게 한다 - 강한 유도와의 차이는 모드로 구분한다.
     */
    private String remainingWorry(StoryScene scene, ProgressionDecision decision) {
        if (decision.guidanceTarget() != null) {
            return scene.getRemainingWorry(decision.guidanceTarget());
        }
        return null;
    }

    private String characterContext(StoryScene scene) {
        return java.util.stream.Stream.of(
                        scene.getCharacterPersona(), scene.getSceneStance(),
                        scene.getSceneDescription(), scene.getConflict())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining("\n"));
    }

    /** 분석 결과를 캐릭터 프롬프트가 읽을 한 덩어리로 줄인다. */
    private String summarize(UtteranceAnalysis analysis) {
        String elements = analysis.getDetectedElements().stream()
                .map(element -> element.type().name())
                .collect(Collectors.joining(", "));
        return "의도=%s / 핵심=%s / 확인된 요소=[%s] / 정보량=%s".formatted(
                analysis.getChildIntent(), analysis.getMainPoint(),
                elements, analysis.getUtteranceValidity());
    }

    /**
     * 캐릭터 LLM이 돌려준 감정 문자열을 아는 값으로 맞춘다.
     * 모르는 값이면 NEUTRAL로 둔다 - 표정 하나 때문에 턴 전체를 실패시킬 이유가 없다.
     */
    private CharacterEmotion toEmotion(String emotion) {
        if (emotion == null) {
            return CharacterEmotion.NEUTRAL;
        }
        try {
            return CharacterEmotion.valueOf(emotion.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CharacterEmotion.NEUTRAL;
        }
    }

    public record CharacterReply(String text, CharacterEmotion emotion) {}
}
