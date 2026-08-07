package com.mugunghwa.goodquestion.infra.llm;

import org.springframework.stereotype.Component;

/** 보호자 리포트 생성 LLM — 세션 대화·분석 결과 종합 → summary/strengths/nextFocus */
@Component
public class ReportLlmClient {

    public ReportLlmResult generate(String conversationDigest) {
        throw new UnsupportedOperationException("TODO");
    }

    public record ReportLlmResult(String summary,
                                  java.util.List<Item> strengths,
                                  java.util.List<Item> nextFocus) {
        public record Item(String element, String comment) {}
    }
}
