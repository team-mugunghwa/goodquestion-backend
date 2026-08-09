package com.mugunghwa.goodquestion.story.dialogue.engine;

import com.mugunghwa.goodquestion.story.dialogue.DetectedElement;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 서버 후처리 (발화 분석 문서 7장) — 새로운 의미를 추가하지 않는 안전장치.
 * LLM 미사용.
 */
@Component
public class AnalysisPostProcessor {

    /**
     * TODO:
     * ① evidence가 아이 최신 발화에 실제 포함되어 있는지 확인 (미포함 요소 삭제)
     * ② 같은 사고 요소가 한 턴에 중복 탐지되면 하나로 정리
     * ③ 스키마(ThinkingElement)에 정의되지 않은 요소 제거
     * ④ 구체성이 부족한 SOLUTION 탐지 보정
     */
    public List<DetectedElement> process(List<DetectedElement> raw, String childUtterance) {
        throw new UnsupportedOperationException("TODO");
    }
}
