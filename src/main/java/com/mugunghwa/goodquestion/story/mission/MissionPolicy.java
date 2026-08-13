package com.mugunghwa.goodquestion.story.mission;

import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.dialogue.UtteranceAnalysis;
import com.mugunghwa.goodquestion.story.session.StorySession;
import lombok.RequiredArgsConstructor;
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
 *
 * <p>콘텐츠의 노출 조건은 사람이 읽는 문장이라 그대로 실행할 수 없다. 위 문장들이 공통으로
 * 말하는 것은 "아이가 한 번은 말했고, 아직 구성할 것이 남았다"이므로 그 두 가지와,
 * 대화가 아직 붙지 않았을 때 오버레이를 띄우지 않기 위한 조건 하나를 규칙으로 옮겼다.
 */
@Component
@RequiredArgsConstructor
public class MissionPolicy {

    /** 아이가 자기 생각을 먼저 말해야 하는 최소 턴 수. */
    private static final int MIN_TURNS_BEFORE_EXPOSURE = 1;
    /** 미션2는 처음부터 보여주면 정해진 답을 찾으려 하므로 한 턴 더 기다린다. */
    private static final int MIN_TURNS_BEFORE_PERSPECTIVE_SHIFT = 2;

    private final MissionConfigReader configReader;

    /**
     * @return 이번 턴에 미션을 노출해야 하면 true.
     *  전제: scene.hasMission() && !session.isMissionExposed()
     */
    public boolean shouldExpose(StorySession session, StoryScene scene, UtteranceAnalysis analysis) {
        // 1) 대화가 아직 붙지 않았다. 되묻는 것이 먼저지 미션이 먼저가 아니다.
        if (analysis.getUtteranceValidity().isLowInformation()) {
            return false;
        }
        // 2) 구성할 것이 남아 있어야 한다. 이미 다 말했으면 미션이 할 일이 없다.
        if (scene.missingElements(session.getAccumulatedElements()).isEmpty()) {
            return false;
        }
        // 3) 아이가 자기 생각을 먼저 말했어야 한다.
        return session.getCurrentChildTurnCount() >= minTurns(scene);
    }

    private int minTurns(StoryScene scene) {
        return configReader.typeOf(scene) == MissionType.PERSPECTIVE_SHIFT
                ? MIN_TURNS_BEFORE_PERSPECTIVE_SHIFT : MIN_TURNS_BEFORE_EXPOSURE;
    }
}
