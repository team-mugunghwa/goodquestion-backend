/**
 * 자체 JWT 인증 — Supabase Auth를 사용하지 않는다(계정-02).
 *
 * <p>구현됨: 이메일 가입·로그인, 카카오 소셜 로그인(인가 코드 서버 교환),
 * 액세스 토큰 발급·검증({@code global.security.JwtProvider}).
 *
 * <p>미구현: 리프레시 토큰 회전과 로그아웃 무효화(계정-05), 구글·네이버 공급자(미결-02).
 *
 * <p>리프레시는 저장소({@link RefreshToken} 엔티티와 {@code refresh_tokens} 테이블)까지
 * 준비돼 있고 발급·회전·무효화 로직만 없다. 그 전까지는 <b>Access 토큰 단일 전략</b>으로
 * 완결 동작한다 — 만료(기본 7일) 시 재로그인으로 재발급한다.
 * 인증 경로 어디도 리프레시 토큰을 참조하지 않으므로 미구현 상태가 다른 기능을 막지 않는다.
 */
package com.mugunghwa.goodquestion.user.auth;
