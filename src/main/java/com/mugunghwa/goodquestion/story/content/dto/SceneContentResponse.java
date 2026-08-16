package com.mugunghwa.goodquestion.story.content.dto;

import com.mugunghwa.goodquestion.story.content.SceneAudio;
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
        /** STORY: 내레이션 사전 렌더 음성. null이면 음성 없이 진행한다 */
        String narrationAudioUrl,
        /**
         * 문장별 실측 시작·끝(초). narrationAudioUrl이 있을 때만 값이 있다.
         *
         * <p>이게 없으면 클라이언트는 글자수 비례로 표시 시간을 추정할 수밖에 없는데,
         * 그러면 문장 길이와 실제 낭독 길이가 어긋나 자막이 소리보다 먼저/늦게 넘어간다.
         */
        List<NarrationTiming> narrationTimings,
        String imageUrl,
        /** 장면 영상. null이면 imageUrl만 쓴다 */
        String videoUrl,
        /** DIALOGUE만 값 */
        String characterName,
        /** DIALOGUE만 값 — 남은 턴 UI에 사용 */
        Short maxTurns
) {

    /** 문장 하나의 실측 구간. index는 narrationSentences의 순서와 같다. */
    public record NarrationTiming(int index, double start, double end) {
    }

    /** 사전 렌더 음성 없이 — 대화 장면이거나 아직 음성이 없는 장면. */
    public static SceneContentResponse from(StoryScene s) {
        return from(s, null);
    }

    public static SceneContentResponse from(StoryScene s, SceneAudio narration) {
        boolean dialogue = s.isDialogue();
        return new SceneContentResponse(
                s.getId(), s.getSceneOrder(), s.getSceneType(),
                dialogue ? List.of() : splitSentences(s.getSceneDescription()),
                narration == null ? null : narration.url(),
                narration == null ? List.of() : toTimings(narration),
                s.getImageUrl(),
                s.getVideoUrl(),
                s.getCharacterName(),
                dialogue ? s.getMaxTurns() : null);
    }

    private static List<NarrationTiming> toTimings(SceneAudio narration) {
        List<SceneAudio.SentenceTiming> timings = narration.getSentenceTimings();
        return timings == null ? List.of() : timings.stream()
                .map(t -> new NarrationTiming(t.index(), t.start(), t.end()))
                .toList();
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
