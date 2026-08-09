/**
 * AI 계약(분석·캐릭터 LLM 입출력)에 등장하는 공용 어휘.
 *
 * <p>여기 있는 enum만이 도메인과 {@code ai} 패키지 양쪽에서 참조된다.
 * {@code ai}가 도메인을 역참조하지 않도록 하는 것이 이 패키지의 존재 이유이므로,
 * AI 계약에 쓰이지 않는 enum(SessionStatus, SceneEndReason, SpeakerType, AuthProvider 등)은
 * 소유 도메인에 그대로 둔다.
 */
package com.mugunghwa.goodquestion.global.vocab;
