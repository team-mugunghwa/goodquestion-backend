package com.mugunghwa.goodquestion.story.dialogue.engine;

import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.story.session.StorySession;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import org.springframework.stereotype.Component;

/**
 * 유도 대상 선택 (발화 분석 문서 12장).
 * 원칙: ① 미충족 필수 요소 우선 ② 직전 유도 요소(last_guidance_target) 반복 금지
 *      ③ 장면 기본 우선순위 참고 ④ 아이가 방금 말한 내용과 자연스럽게 연결되는 요소 우선
 */
@Component
public class GuidanceTargetSelector {

    public ThinkingElement select(StorySession session, StoryScene scene) {
        throw new UnsupportedOperationException("TODO");
    }
}
