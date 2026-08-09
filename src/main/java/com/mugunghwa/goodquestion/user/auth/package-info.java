/**
 * 자체 JWT 인증 — Supabase Auth를 사용하지 않는다(계정-02).
 *
 * <p>구현됨: 이메일 가입·로그인, 카카오 소셜 로그인(인가 코드 서버 교환),
 * 액세스 토큰 발급·검증({@code global.security.JwtProvider}).
 *
 * <p>미구현: 리프레시 토큰 회전과 로그아웃 무효화(계정-05 — RefreshToken 엔티티 필요),
 * 구글·네이버 공급자(미결-02에서 시작 순서를 열어둔 상태).
 */
package com.mugunghwa.goodquestion.user.auth;
