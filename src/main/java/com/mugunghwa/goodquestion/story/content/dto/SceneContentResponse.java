package com.mugunghwa.goodquestion.story.content.dto;

import com.mugunghwa.goodquestion.story.content.SceneType;
import com.mugunghwa.goodquestion.story.content.StoryScene;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 명세 3-7 장면 콘텐츠 — 세션 시작·이어하기·스토리 완료 보고·현재 장면 조회가 공유한다.
 *
 * <p>element_criteria·remaining_worries·mission_config 같은 서버 내부 설정은 의도적으로 뺀다.
 * 미션은 노출 시점에 턴 처리 응답으로 전달한다.
 */
public record SceneContentResponse(
        UUID sceneId,
        short sceneOrder,
        SceneType sceneType,
        /** STORY: 한 문장씩 순차 표시용(장면-05) / DIALOGUE: 빈 배열 */
        List<String> narrationSentences,
        String imageUrl,
        /** DIALOGUE만 값 */
        String characterName,
        /** DIALOGUE만 값 — 남은 턴 UI에 사용 */
        Short maxTurns
) {

    public static SceneContentResponse from(StoryScene s) {
        boolean dialogue = s.isDialogue();
        return new SceneContentResponse(
                s.getId(), s.getSceneOrder(), s.getSceneType(),
                dialogue ? List.of() : splitSentences(s.getSceneDescription()),
                s.getImageUrl(),
                s.getCharacterName(),
                dialogue ? s.getMaxTurns() : null);
    }

    /**
     * 내레이션을 줄바꿈 기준으로 나눈다.
     * 마침표로 자르면 "1.5km" 같은 표현이 깨지므로 콘텐츠 작성 시 줄바꿈으로 구분하는 규칙을 따른다.
     */
    private static List<String> splitSentences(String description) {
        if (description == null || description.isBlank()) {
            return List.of();
        }
        return Arrays.stream(description.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }
}
