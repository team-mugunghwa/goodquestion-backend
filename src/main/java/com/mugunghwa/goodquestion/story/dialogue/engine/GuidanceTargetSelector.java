package com.mugunghwa.goodquestion.story.dialogue.engine;

import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.session.StorySession;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 유도 대상 선택 (발화 분석 문서 12장).
 * 원칙: ① 미충족 필수 요소 우선 ② 직전 유도 요소(last_guidance_target) 반복 금지
 *      ③ 장면 기본 우선순위 참고 ④ 아이가 방금 말한 내용과 자연스럽게 연결되는 요소 우선
 *
 * <p>원칙 4)는 의미 판단이 필요해 규칙으로 옮기지 않았다. 대신 장면이 선언한 요소 순서를
 * 따른다 - 콘텐츠가 이야기 흐름대로 요소를 적어 두므로 그 순서가 곧 자연스러운 순서다.
 */
@Component
public class GuidanceTargetSelector {

    /**
     * @return 유도할 요소. 미충족 요소가 없으면 null (유도할 것이 없다는 뜻이므로 호출부가 처리한다)
     */
    public ThinkingElement select(StorySession session, StoryScene scene) {
        List<ThinkingElement> missing = scene.missingElements(session.getAccumulatedElements());
        if (missing.isEmpty()) {
            return null;
        }

        // 직전과 같은 요소를 다시 유도하면 캐릭터가 같은 걱정을 반복하게 된다.
        // 다른 선택지가 없으면 어쩔 수 없이 같은 요소로 돌아간다.
        ThinkingElement last = session.getLastGuidanceTarget();
        return missing.stream()
                .filter(element -> element != last)
                .findFirst()
                .orElse(missing.getFirst());
    }
}
