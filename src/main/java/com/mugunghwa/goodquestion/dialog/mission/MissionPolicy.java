package com.mugunghwa.goodquestion.dialog.mission;

import com.mugunghwa.goodquestion.dialog.analysis.UtteranceAnalysis;
import com.mugunghwa.goodquestion.session.session.StorySession;
import com.mugunghwa.goodquestion.story.scene.StoryScene;
import org.springframework.stereotype.Component;

/**
 * 미션 노출 판단 (콘텐츠 문서 '미션 노출 원칙') — LLM 미사용 규칙.
 * 원칙: 대화 시작과 동시에 노출하지 않고, 해결 방법을 실제로 구성해야 하는 시점에 노출.
 *
 * 노출 조건 (scene.mission_config.exposure_conditions 참고):
 *  · 아이가 해결 방향은 말했지만 방법이 구체적이지 않은 경우
 *  · 일정 턴 이상 대화했지만 실행 방법(핵심 요소)이 나오지 않은 경우
 *  · 캐릭터 질문만으로 구체화하기 어려운 경우
 * 미션2는 아이가 자기 생각을 먼저 말한 뒤 노출 (정답 찾기 방지).
 */
@Component
public class MissionPolicy {

    /**
     * @return 이번 턴에 미션을 노출해야 하면 true.
     *  전제: scene.hasMission() && !session.isMissionExposed()
     */
    public boolean shouldExpose(StorySession session, StoryScene scene, UtteranceAnalysis analysis) {
        // TODO: mission_config의 노출 조건 + 누적 상태(turn 수, 미충족 요소)로 판단
        throw new UnsupportedOperationException("TODO");
    }
}
