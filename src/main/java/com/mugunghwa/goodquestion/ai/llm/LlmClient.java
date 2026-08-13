package com.mugunghwa.goodquestion.ai.llm;

import tools.jackson.databind.JsonNode;

import java.util.Map;

/**
 * LLM 공급자 호출 공통 포트. analysis·character·report·word 어댑터가 공유한다.
 *
 * <p>구조화 출력만 지원한다. 이 서비스의 LLM 출력은 전부 서버가 파싱해 판단에 쓰므로
 * (분석 요소, 캐릭터 감정) 자유 텍스트 응답을 받을 일이 없고, 스키마를 강제해야
 * "형식이 어긋나 턴이 실패하는" 경우가 줄어든다.
 */
public interface LlmClient {

    /**
     * 시스템/사용자 프롬프트로 JSON 응답을 받는다. <b>트랜잭션 밖에서 부를 것.</b>
     *
     * @param schemaName 공급자에 전달할 스키마 이름 (영문·언더스코어)
     * @param schema     JSON Schema (Map 표현). 응답은 이 스키마를 따르도록 강제된다
     * @return 파싱된 JSON과 실제 사용된 모델 식별자
     */
    LlmJsonResult completeJson(String systemPrompt, String userPrompt,
                               String schemaName, Map<String, Object> schema);

    record LlmJsonResult(JsonNode json, String modelId) {}
}
