package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.story.session.StorySession;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * CLOSING 처리.
 * character_closing이 있으면 고정 대사 재생, 없으면 캐릭터 LLM이 마무리 대사 생성
 * (문서 상충 → 서버 정책으로 분기하기로 결정).
 * 이후 다음 장면 이동(opening 저장) 또는 마지막 장면이면 후속 활동 전환.
 */
@Service
@RequiredArgsConstructor
public class SceneClosingHandler {

    public void close(StorySession session, StoryScene scene) {
        throw new UnsupportedOperationException("TODO");
    }
}
