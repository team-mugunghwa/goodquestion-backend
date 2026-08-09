package com.mugunghwa.goodquestion.story.session;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.story.session.dto.CurrentSceneResponse;
import com.mugunghwa.goodquestion.story.session.dto.SceneOpeningResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 장면 재생 흐름(장면-01~10).
 *
 * <p>스토리 재생 완료 보고(story-complete)는 세션 상태를 전이시키므로
 * {@link SessionController}에 이미 구현되어 있다. TODO: 아래 두 개 구현.
 */
@RestController
@RequestMapping("/api/sessions/{sessionId}/scenes/current")
@RequiredArgsConstructor
public class SceneFlowController {

    /** 고정 첫 대사를 재생한다. "ㅇㅇ"를 아이 이름으로 치환하며 멱등이다(캐릭터-14, 17). */
    @PostMapping("/opening")
    public SceneOpeningResponse playOpening(@CurrentParentId UUID parentId,
                                            @PathVariable UUID sessionId) {
        throw new UnsupportedOperationException("미구현: 장면 첫 대사 재생");
    }

    @GetMapping
    public CurrentSceneResponse getCurrentScene(@CurrentParentId UUID parentId,
                                                @PathVariable UUID sessionId) {
        throw new UnsupportedOperationException("미구현: 현재 장면 콘텐츠 조회");
    }
}
