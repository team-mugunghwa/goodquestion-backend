package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.ai.character.CharacterLlmClient;
import com.mugunghwa.goodquestion.global.vocab.CharacterEmotion;
import com.mugunghwa.goodquestion.global.vocab.ReactionKey;
import com.mugunghwa.goodquestion.global.vocab.ResponseMode;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.dialogue.engine.ProgressionDecision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 캐릭터 응답.
 * 전달 정보: 아이 최신 발화, 분석 결과, 진행 모드, 캐릭터 감정·상황,
 * (유도 시) scene.getRemainingWorry(guidanceTarget) — 요소별 캐릭터의 남은 걱정.
 * 원칙: 직접적 학습 질문 금지 — 캐릭터의 상황·감정 안에서 걱정을 드러낸다.
 *
 * <p>무엇을 말할지는 이미 정해진 상태로 들어온다. 여기서 하는 일은 결정된 모드와 유도 대상을
 * 캐릭터 프롬프트 입력으로 옮기고, 돌아온 감정 문자열을 아는 값으로 맞추는 것뿐이다 -
 * 판정을 LLM에 넘기지 않는다는 원칙(정책-02)이 이 경계에서 지켜진다.
 *
 * <p>프롬프트 조립({@link #promptFor})과 호출({@link #reply})이 나뉘어 있다. 조립은 장면
 * 엔티티를 읽어야 해서 트랜잭션 안에서, 호출은 수 초가 걸려서 트랜잭션 밖에서 일어난다.
 */
@Service
@RequiredArgsConstructor
public class CharacterResponseService {

    private final CharacterLlmClient characterLlmClient;

    /** 트랜잭션 안에서 조립한다 - 장면·캐릭터 엔티티가 붙어 있어야 읽을 수 있다. */
    public CharacterPrompt promptFor(StoryScene scene, AnalysisOutcome analysis,
                                     ProgressionDecision decision, ReactionKey reactionKey,
                                     String childUtterance) {
        return new CharacterPrompt(
                childUtterance,
                summarize(analysis),
                decision.mode(),
                reactionKey,
                scene.getCharacterName(),
                characterContext(scene),
                remainingWorry(scene, decision),
                decision.softCue(),
                guidanceStyle(scene),
                fixedText(scene, decision));
    }

    /** 걱정을 어떻게 드러낼지의 캐릭터별 표현 방식(캐릭터-11). 캐릭터 참조가 없으면 null. */
    private String guidanceStyle(StoryScene scene) {
        return scene.getCharacter() != null ? scene.getCharacter().getGuidanceStyle() : null;
    }

    /**
     * <b>트랜잭션 밖에서 부른다.</b>
     *
     * <p>고정 마지막 대사가 있는 장면의 CLOSING 턴은 LLM을 부르지 않는다 - 장면을 어떻게
     * 끝냈든 이야기는 같은 자리로 돌아와야 다음 장면이 이어진다. 감정도 붙이지 않는다.
     */
    public CharacterReply reply(CharacterPrompt prompt) {
        if (prompt.hasFixedText()) {
            return new CharacterReply(prompt.fixedText(), null);
        }
        CharacterLlmClient.CharacterLlmResult result = characterLlmClient.reply(
                new CharacterLlmClient.CharacterLlmInput(
                        prompt.childUtterance(), prompt.analysisSummary(), prompt.mode().name(),
                        prompt.reactionKey().name(), prompt.characterName(), prompt.characterContext(),
                        prompt.remainingWorry(), prompt.softCue(), prompt.guidanceStyle()));

        return new CharacterReply(result.text(), toEmotion(result.emotion()));
    }

    /** 고정 마무리 대사가 없는 장면은 CLOSING에서도 LLM이 마무리 대사를 만든다. */
    private String fixedText(StoryScene scene, ProgressionDecision decision) {
        return decision.mode() == ResponseMode.CLOSING ? scene.getCharacterClosing() : null;
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

    /**
     * 캐릭터 공통 성격(characters.personality)과 장면별 입장(scene_stance)을 합친다.
     * 장면 상황·갈등까지 이어 붙여 캐릭터가 지금 왜 이런 태도인지를 프롬프트에 담는다.
     */
    private String characterContext(StoryScene scene) {
        String personality = scene.getCharacter() != null
                ? scene.getCharacter().getPersonality() : null;
        return Stream.of(personality, scene.getSceneStance(),
                        scene.getSceneDescription(), scene.getConflict())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining("\n"));
    }

    /** 분석 결과를 캐릭터 프롬프트가 읽을 한 덩어리로 줄인다. */
    private String summarize(AnalysisOutcome analysis) {
        String elements = analysis.detectedElements().stream()
                .map(element -> element.type().name())
                .collect(Collectors.joining(", "));
        return "의도=%s / 핵심=%s / 확인된 요소=[%s] / 정보량=%s".formatted(
                analysis.childIntent(), analysis.mainPoint(),
                elements, analysis.utteranceValidity());
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

    /** 아이 이름 치환 전 원문이다. 치환은 저장 직전 트랜잭션 안에서 한다. */
    public record CharacterReply(String text, CharacterEmotion emotion) {}
}
