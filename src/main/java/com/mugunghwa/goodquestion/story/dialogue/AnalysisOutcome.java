package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.global.vocab.ChildIntent;
import com.mugunghwa.goodquestion.global.vocab.UtteranceValidity;

import java.util.List;

/**
 * 분석 LLM 호출과 서버 후처리까지 끝난 결과. 아직 저장되지 않았다.
 *
 * <p>LLM 호출을 트랜잭션 밖으로 뺀 뒤로 "분석은 끝났지만 저장할 트랜잭션은 아직 열리지 않은"
 * 구간이 생긴다. 그 구간을 건너는 값이다. 저장은 {@link UtteranceAnalysisService#save}가 한다.
 */
public record AnalysisOutcome(ChildIntent childIntent,
                              String mainPoint,
                              List<DetectedElement> detectedElements,
                              List<DetectedElement> droppedEvidence,
                              UtteranceValidity utteranceValidity,
                              String modelId) {

    /** 세션 누적 상태가 쓰는 요소 이름 목록. */
    public List<String> elementNames() {
        return detectedElements.stream().map(element -> element.type().name()).toList();
    }

    public boolean lowInformation() {
        return utteranceValidity.isLowInformation();
    }
}
