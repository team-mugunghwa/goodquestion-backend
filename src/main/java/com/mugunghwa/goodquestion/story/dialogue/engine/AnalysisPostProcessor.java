package com.mugunghwa.goodquestion.story.dialogue.engine;

import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.story.dialogue.DetectedElement;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 서버 후처리 (발화 분석 문서 7장) — 새로운 의미를 추가하지 않는 안전장치.
 * LLM 미사용.
 *
 * <p>여기서 하는 일은 걸러내기뿐이다. 분석 LLM이 제안한 요소 중 근거가 확인되지 않는 것을
 * 버리고 나머지를 그대로 통과시킨다. 요소를 보태거나 종류를 바꾸지 않는다 -
 * 그 순간 "LLM이 뭘 봤는지"를 사후에 추적할 수 없게 된다.
 */
@Component
public class AnalysisPostProcessor {

    /**
     * 구체성을 인정할 최소 근거 길이(공백·문장부호 제거 기준).
     * SOLUTION은 "무엇을 어떻게"가 담겨야 하는데 이보다 짧으면 담길 수가 없다.
     */
    private static final int MIN_SOLUTION_EVIDENCE_LENGTH = 6;

    /**
     * 방법이 없는 막연한 당위 표현. 이것만으로는 해결 방법을 구성했다고 보지 않는다.
     * TODO: 실제 아동 발화 로그가 쌓이면 목록을 넓힌다. 지금은 명백한 것만 담았다.
     */
    private static final Set<String> VAGUE_SOLUTION_PHRASES = Set.of(
            "잘하면돼요", "잘하면된다", "열심히하면돼요", "열심히하면된다",
            "어떻게든해요", "어떻게든하면돼요", "알아서해요", "그냥하면돼요");

    /**
     * 분석 LLM이 제안한 요소를 검증해 통과분과 폐기분으로 가른다.
     *
     * <p>순서에 의미가 있다. 중복 정리를 먼저 하면 앞에 온 무효 요소가 자리를 차지해
     * 뒤에 오는 유효한 같은 종류 요소까지 함께 버려진다.
     *
     * @param raw            분석 LLM 출력에서 스키마에 맞게 변환된 요소들
     * @param childUtterance 이번 턴 아이 발화 원문
     */
    public Result process(List<DetectedElement> raw, String childUtterance) {
        String utterance = normalize(childUtterance);
        List<DetectedElement> accepted = new ArrayList<>();
        List<DetectedElement> dropped = new ArrayList<>();
        Set<ThinkingElement> alreadyAccepted = EnumSet.noneOf(ThinkingElement.class);

        for (DetectedElement element : raw != null ? raw : List.<DetectedElement>of()) {
            // 스키마에 없는 요소는 기록할 자리도 없다. 변환 단계에서 이미 걸러지지만 방어한다.
            if (element == null || element.type() == null) {
                continue;
            }
            // 아이가 실제로 한 말에 근거가 없으면 LLM이 지어낸 것이다.
            if (!containsEvidence(utterance, element.evidence())) {
                dropped.add(element);
                continue;
            }
            // 방법이 없는 당위 표현을 해결책으로 세지 않는다.
            if (element.type() == ThinkingElement.SOLUTION && !isConcrete(element.evidence())) {
                dropped.add(element);
                continue;
            }
            // 같은 요소가 한 턴에 여러 번 잡히면 첫 근거만 남긴다.
            if (!alreadyAccepted.add(element.type())) {
                dropped.add(element);
                continue;
            }
            accepted.add(element);
        }

        return new Result(List.copyOf(accepted), List.copyOf(dropped));
    }

    /**
     * @param accepted 누적 요소로 인정할 것
     * @param dropped  폐기한 것. 분석 LLM이 없는 요소를 만들어내는 빈도를 추적하려고 남긴다
     */
    public record Result(List<DetectedElement> accepted, List<DetectedElement> dropped) {

        /** 세션 누적 상태가 쓰는 요소 이름 목록. */
        public List<String> acceptedElementNames() {
            return accepted.stream().map(e -> e.type().name()).toList();
        }
    }

    /**
     * 근거가 발화 안에 실제로 있는지 확인한다.
     *
     * <p>공백과 문장부호를 지우고 비교한다. LLM은 근거를 원문 그대로 옮기라는 지시를 받지만
     * 띄어쓰기나 마침표를 다듬어 내놓는 일이 흔하고, 그 정도 차이로 유효한 탐지를 버리면
     * 걸러내기가 아니라 손실이 된다.
     */
    private boolean containsEvidence(String normalizedUtterance, String evidence) {
        String normalizedEvidence = normalize(evidence);
        return !normalizedEvidence.isEmpty() && normalizedUtterance.contains(normalizedEvidence);
    }

    private boolean isConcrete(String evidence) {
        String normalized = normalize(evidence);
        return normalized.length() >= MIN_SOLUTION_EVIDENCE_LENGTH
                && !VAGUE_SOLUTION_PHRASES.contains(normalized);
    }

    /** 공백과 문장부호를 지운다. 한글·영숫자만 남긴다. */
    private String normalize(String text) {
        return text == null ? "" : text.replaceAll("[^가-힣ㄱ-ㅎㅏ-ㅣa-zA-Z0-9]", "");
    }
}
