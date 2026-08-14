package com.mugunghwa.goodquestion.ai.llm;

import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.Map;

/**
 * LLM 공급자 호출 공통 포트. analysis·character·report·word 어댑터가 공유한다.
 *
 * <p>구조화 출력만 지원한다. 이 서비스의 LLM 출력은 전부 서버가 파싱해 판단에 쓰므로
 * (분석 요소, 캐릭터 감정) 자유 텍스트 응답을 받을 일이 없고, 스키마를 강제해야
 * "형식이 어긋나 턴이 실패하는" 경우가 줄어든다.
 */
public interface LlmClient {

    /** 공용 타임아웃으로 호출한다. 대화 중 호출은 전부 이쪽을 쓴다. */
    default LlmJsonResult completeJson(String systemPrompt, String userPrompt,
                                       String schemaName, Map<String, Object> schema) {
        return completeJson(systemPrompt, userPrompt, schemaName, schema, null);
    }

    /**
     * 시스템/사용자 프롬프트로 JSON 응답을 받는다. <b>트랜잭션 밖에서 부를 것.</b>
     *
     * @param schemaName 공급자에 전달할 스키마 이름 (영문·언더스코어)
     * @param schema     JSON Schema (Map 표현). 응답은 이 스키마를 따르도록 강제된다
     * @param timeout    응답 타임아웃. null이면 공용 WebClient 설정을 따른다.
     *                   리포트처럼 대화 전체를 넣고 긴 JSON을 받는 작업은 공용 값으로는
     *                   끊긴다. 반대로 대화 턴은 아이가 마이크 앞에서 기다리는 시간이
     *                   품질보다 비싸 짧게 두어야 하므로, 공용 값을 늘리는 대신 여기서 준다
     * @return 파싱된 JSON과 실제 사용된 모델 식별자
     */
    LlmJsonResult completeJson(String systemPrompt, String userPrompt,
                               String schemaName, Map<String, Object> schema,
                               Duration timeout);

    record LlmJsonResult(JsonNode json, String modelId) {}
}