/**
 * 홈 화면 조립 — 여러 도메인을 가로지르는 읽기 전용 조회.
 *
 * <p>이어하기(story) + 추천(story) + 섬 위젯(learning.reward) + 아이(user)를 한 응답으로 묶는다.
 * 도메인 위에 얹힌 조립 계층이므로 {@code story}·{@code learning} 양쪽을 의존해도
 * learning → story 의존 방향을 깨지 않는다. 어떤 도메인도 이 패키지를 의존하지 않는다.
 * 여기에는 조회만 두고 상태 변경은 각 도메인 서비스에 맡긴다.
 */
package com.mugunghwa.goodquestion.home;
