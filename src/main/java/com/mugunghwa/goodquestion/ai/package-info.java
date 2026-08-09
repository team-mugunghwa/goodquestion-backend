/**
 * LLM·STT·TTS 어댑터. 무상태이며 엔티티·리포지토리를 두지 않는다.
 *
 * <p><b>의존 규칙</b>: {@code story}·{@code learning} → {@code ai} 단방향.
 * {@code ai}는 어떤 도메인 패키지도 import 하지 않으며,
 * 필요한 값은 {@code global.vocab} enum과 자체 DTO로만 주고받는다.
 * 유스케이스별(analysis·character·report·word) 하위 패키지에 포트·구현·프롬프트를 함께 둔다.
 */
package com.mugunghwa.goodquestion.ai;
