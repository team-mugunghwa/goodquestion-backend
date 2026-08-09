/**
 * 서버 규칙 엔진 — 진행 판단·유도 대상 선택·분석 후처리.
 *
 * <p>정책-02에 따라 종료·목표 달성·유도 여부는 전부 여기서 결정하며 LLM에 위임하지 않는다.
 * LLM 호출을 직접 하지 않는 순수 규칙 코드로 유지해 단위 테스트 가능하게 둔다.
 */
package com.mugunghwa.goodquestion.story.dialogue.engine;
