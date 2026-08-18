package com.mugunghwa.goodquestion.global.vocab;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 보호자 리포트 6각 그래프 축. 엔진이 이미 추출하는 {@link ThinkingElement}(8종)를
 * 보호자용 6축으로 압축한 매핑이다 — 새 분석 로직·새 LLM 호출 없음(D6 A안).
 *
 * <p>축 순서가 곧 육각형 배치 순서다(12시부터 시계방향).
 * 설계 근거: claude/보호자리포트_6축그래프_설계안_D6.md
 */
@Getter
@RequiredArgsConstructor
public enum ReportAxis {
    REASON_TAKING("이유대기", "왜 그렇게 생각했는지 근거를 붙여 말해요",
            List.of(ThinkingElement.REASON)),
    RESULT_PREDICTION("결과예측", "그 행동 다음에 벌어질 일을 미리 그려봐요",
            List.of(ThinkingElement.RESULT)),
    DECISION_MAKING("판단력", "상황을 보고 스스로 판단·선택을 말해요",
            List.of(ThinkingElement.DECISION)),
    PROBLEM_SOLVING("해결력", "문제를 줄일 방법이나 바람을 제안해요",
            List.of(ThinkingElement.SOLUTION, ThinkingElement.REQUEST)),
    PERSPECTIVE_TAKING("관점이해", "다른 인물의 입장과 마음을 헤아려요",
            List.of(ThinkingElement.PERSPECTIVE, ThinkingElement.EMPATHY)),
    EMOTION_EXPRESSION("감정표현", "자기 감정을 자기 말로 표현해요",
            List.of(ThinkingElement.EMOTION));

    private final String label;
    private final String description;
    private final List<ThinkingElement> elements;

    private static final Map<ThinkingElement, ReportAxis> BY_ELEMENT = Arrays.stream(values())
            .flatMap(axis -> axis.elements.stream().map(element -> Map.entry(element, axis)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    /** 엔진 요소 하나가 속한 축을 찾는다. 8종 전부 어딘가에 매핑되어 있어 null이 되지 않는다. */
    public static ReportAxis of(ThinkingElement element) {
        return BY_ELEMENT.get(element);
    }
}
