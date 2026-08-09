/**
 * 대화 장면의 발화 턴 처리 파이프라인.
 *
 * <p>흐름: 세션 검증·락 → 아이 메시지 저장 → 분석 LLM(트랜잭션 밖) → 후처리
 * → 세션 누적 갱신 → 모드 결정 → 미션 노출 판정 → 캐릭터 대사 또는 고정 마지막 대사.
 * 판정·종료 결정은 {@code engine}이 하고 LLM은 분석·표현만 담당한다(정책-01, 정책-02).
 */
package com.mugunghwa.goodquestion.story.dialogue;
