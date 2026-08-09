package com.mugunghwa.goodquestion.story.dialogue.engine;

import com.mugunghwa.goodquestion.story.dialogue.UtteranceAnalysis;
import com.mugunghwa.goodquestion.story.session.StorySession;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 진행 규칙 엔진 (발화 분석 문서 10~11장) — LLM 미사용, 순수 규칙.
 * 인프라 의존 없이 단위 테스트 가능하게 유지할 것.
 */
@Component
@RequiredArgsConstructor
public class ProgressionEngine {

    private final GuidanceTargetSelector guidanceTargetSelector;

    /**
     * 판단 순서 (문서 11장):
     * 1. 종료 조건 — (필수 요소 충족 && 최소 대화량[preferred_turns] 충족) → CLOSING(GOAL_MET)
     *               / 최대 대화 범위[max_turns] 도달 → CLOSING(MAX_TURNS)
     * 2. 강한 유도 제한 — 첫 발화 / 이번 턴 새 요소 확인됨 / 직전 턴이 GUIDED → GUIDED 금지
     * 3. 유도 필요성 — 필수 요소 잔여 && (turns_without_new_element >= 2
     *               || consecutive_low_information_turns >= 2 || 남은 대화 기회 부족) → GUIDED
     * 4. 그 외 → NORMAL
     */
    public ProgressionDecision decide(StorySession session, StoryScene scene, UtteranceAnalysis analysis) {
        // TODO: 위 순서대로 구현. 임계값(2회)은 DB 문서 참고사항 기준, 운영 설정으로 분리 가능.
        throw new UnsupportedOperationException("TODO");
    }
}
