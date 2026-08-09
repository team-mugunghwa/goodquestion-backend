package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.story.dialogue.engine.ProgressionDecision;
import com.mugunghwa.goodquestion.ai.character.CharacterLlmClient;
import com.mugunghwa.goodquestion.global.vocab.CharacterEmotion;
import com.mugunghwa.goodquestion.story.session.StorySession;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 캐릭터 응답 (NORMAL/GUIDED).
 * 전달 정보: 아이 최신 발화, 분석 결과, 진행 모드, 캐릭터 감정·상황,
 * (유도 시) scene.getRemainingWorry(guidanceTarget) — 요소별 캐릭터의 남은 걱정.
 * 원칙: 직접적 학습 질문 금지 — 캐릭터의 상황·감정 안에서 걱정을 드러낸다.
 */
@Service
@RequiredArgsConstructor
public class CharacterResponseService {

    private final CharacterLlmClient characterLlmClient;

    public CharacterReply reply(StorySession session, StoryScene scene,
                                UtteranceAnalysis analysis, ProgressionDecision decision) {
        // TODO: GUIDED면 scene.getRemainingWorry(decision.guidanceTarget())를 프롬프트에 포함,
        //       NORMAL이면 약한 유도(남은 걱정 가볍게 노출)를 선택적으로 사용.
        //       응답의 emotion 문자열 → CharacterEmotion 매핑 (미매핑 시 NEUTRAL 폴백)
        throw new UnsupportedOperationException("TODO");
    }

    public record CharacterReply(String text, CharacterEmotion emotion) {}
}
