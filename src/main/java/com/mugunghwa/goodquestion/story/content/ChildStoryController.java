package com.mugunghwa.goodquestion.story.content;

import com.mugunghwa.goodquestion.global.security.CurrentParentId;
import com.mugunghwa.goodquestion.story.content.dto.CompletedStoriesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 아이별로 달라지는 이야기 정보.
 *
 * <p>{@link StoryController}와 갈라 둔 이유 - 저쪽({@code /api/stories})은 <b>누가 보든
 * 같은 답</b>을 주는 콘텐츠 조회다. 여기는 아이 한 명에 매인 답이라 소유권 검증이 붙는다.
 * 경로가 {@code /api/children/{childId}/...} 로 갈려 있으면 아이가 필요한 조회인지
 * 경로만 보고 알 수 있다. 자유 대화({@code FreeTalkController})가 아이 소유 경로와 대화
 * 경로를 나눈 것과 같은 규칙이다.
 */
@RestController
@RequiredArgsConstructor
public class ChildStoryController {

    private final StoryService storyService;

    /**
     * 이 아이가 완주한 이야기의 id들. 목록 화면이 카드에 "끝냈어" 도장을 찍는 데 쓴다.
     *
     * <p>한 번도 완주하지 않았으면 빈 배열이다 - 404가 아니다. 아이가 아직 아무것도
     * 안 한 것은 정상이고, 목록 화면은 그 응답으로 도장만 안 찍으면 된다.
     */
    @GetMapping("/api/children/{childId}/stories/completed")
    public CompletedStoriesResponse getCompletedStories(@CurrentParentId UUID parentId,
                                                        @PathVariable UUID childId) {
        return storyService.getCompletedStories(parentId, childId);
    }
}
