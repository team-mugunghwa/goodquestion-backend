/**
 * LLM 공급자 호출 공통 계층.
 *
 * <p>analysis·character·report·word 어댑터가 공유하는 {@code LlmClient} 포트와
 * OpenAI 구현체({@code OpenAiLlmClient}, gpt-5-mini, json_schema 구조화 출력)를 둔다.
 * 다른 공급자로 바뀌면 구현체만 교체한다.
 */
package com.mugunghwa.goodquestion.ai.llm;
