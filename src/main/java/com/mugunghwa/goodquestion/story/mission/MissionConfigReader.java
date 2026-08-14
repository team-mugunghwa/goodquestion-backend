package com.mugunghwa.goodquestion.story.mission;

import com.mugunghwa.goodquestion.story.content.StoryScene;
import com.mugunghwa.goodquestion.story.mission.dto.MissionResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 장면의 {@code mission_config}(jsonb)를 화면이 쓰는 {@link MissionResponse}로 옮긴다.
 *
 * <p>미션은 콘텐츠라 코드에 심지 않는다. 대신 jsonb는 형태를 강제하지 못하므로
 * 읽는 규칙을 이 한 곳에 모아 둔다 - 여러 곳에서 제각기 꺼내 쓰면 콘텐츠가 조금 바뀔 때마다
 * 어디가 깨지는지 알 수 없게 된다.
 *
 * <p>questions·cards는 콘텐츠에 명시하는 것이 원칙이다. 명시가 없으면 사람이 읽으라고 적어 둔
 * check_points·examples에서 만들어 내되, 그때는 화면 문구가 다듬어지지 않은 상태로 나간다.
 */
@Component
public class MissionConfigReader {

    /** 미션1 답변 칸의 고정 키(미션-04). 콘텐츠의 확인 항목 순서와 짝을 이룬다. */
    private static final List<String> QUESTION_KEYS = List.of("tool", "reason", "request", "expectedResult");

    public String missionIdOf(StoryScene scene) {
        return string(config(scene), "mission_id");
    }

    /**
     * 장면이 닫힐 때 끼워 넣을 결과 연출 이미지(대화3의 "배가 떨어지는" 연출, 2026-08 확정).
     * 미션 소속 콘텐츠라 mission_config에 두지만, 노출/완료 여부와 무관하게 장면 종료
     * 전환에 항상 실린다 - 마지막 대사가 결과를 전제하므로 어떤 사유로 닫혀도 연출은 같다.
     * 없으면 null.
     */
    public String resultImageUrlOf(StoryScene scene) {
        return string(config(scene), "result_image_url");
    }

    /**
     * 미션 유형. 콘텐츠에 {@code mission_type}이 있으면 그대로 쓰고,
     * 없으면 확인 항목(check_points)을 가진 쪽을 문제 해결 미션으로 본다.
     */
    public MissionType typeOf(StoryScene scene) {
        Map<String, Object> config = config(scene);
        String declared = string(config, "mission_type");
        if (declared != null) {
            return MissionType.valueOf(declared);
        }
        return config.containsKey("check_points")
                ? MissionType.PROBLEM_SOLVING : MissionType.PERSPECTIVE_SHIFT;
    }

    /** 노출 턴에 화면으로 내려보낼 미션. 미션이 없는 장면이면 null. */
    public MissionResponse toResponse(StoryScene scene) {
        if (!scene.hasMission()) {
            return null;
        }
        Map<String, Object> config = config(scene);
        MissionType type = typeOf(scene);

        return new MissionResponse(
                string(config, "mission_id"),
                type,
                string(config, "name"),
                string(config, "purpose"),
                type == MissionType.PROBLEM_SOLVING
                        ? new MissionResponse.Payload(questions(config), null)
                        : new MissionResponse.Payload(null, cards(config)));
    }

    private List<MissionResponse.Question> questions(Map<String, Object> config) {
        List<Map<String, Object>> declared = list(config, "questions");
        if (!declared.isEmpty()) {
            return declared.stream()
                    .map(q -> new MissionResponse.Question(string(q, "key"), string(q, "label")))
                    .toList();
        }
        // 콘텐츠에 명시가 없으면 확인 항목을 순서대로 고정 키에 붙인다.
        List<Object> checkPoints = rawList(config, "check_points");
        return java.util.stream.IntStream.range(0, Math.min(checkPoints.size(), QUESTION_KEYS.size()))
                .mapToObj(i -> new MissionResponse.Question(
                        QUESTION_KEYS.get(i), String.valueOf(checkPoints.get(i))))
                .toList();
    }

    private List<MissionResponse.Card> cards(Map<String, Object> config) {
        List<Map<String, Object>> declared = list(config, "cards");
        if (!declared.isEmpty()) {
            return declared.stream()
                    .map(c -> new MissionResponse.Card(string(c, "key"), string(c, "label"),
                            string(c, "image_url"), string(c, "template")))
                    .toList();
        }
        // 콘텐츠에 명시가 없으면 예시 문장을 그대로 카드 라벨로 쓴다.
        List<Object> examples = rawList(config, "examples");
        return java.util.stream.IntStream.range(0, examples.size())
                .mapToObj(i -> new MissionResponse.Card(
                        "card_" + (i + 1), String.valueOf(examples.get(i)), null, null))
                .toList();
    }

    private Map<String, Object> config(StoryScene scene) {
        return scene.getMissionConfig() != null ? scene.getMissionConfig() : Map.of();
    }

    private String string(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value != null ? String.valueOf(value) : null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> list(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value instanceof List<?> items
                ? (List<Map<String, Object>>) items : List.of();
    }

    private List<Object> rawList(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value instanceof List<?> items ? List.copyOf(items) : List.of();
    }
}
