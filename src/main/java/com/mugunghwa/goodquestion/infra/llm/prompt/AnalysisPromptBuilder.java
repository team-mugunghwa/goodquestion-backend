package com.mugunghwa.goodquestion.infra.llm.prompt;

import org.springframework.stereotype.Component;

/**
 * 발화 분석 프롬프트 조립.
 * 기본 판정 원칙(문서 5장) 포함: 직접 확인 요소만 탐지, 추론 금지, 원문 근거 필수,
 * 복수 탐지 허용, 장면 무관 요소 배제, 막연한 당위 표현 과대평가 금지.
 */
@Component
public class AnalysisPromptBuilder {
    // TODO: 시스템 프롬프트 템플릿 + 입력 JSON 조립
}
