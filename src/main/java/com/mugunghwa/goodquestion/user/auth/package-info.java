/**
 * 자체 JWT 인증 — Supabase Auth를 사용하지 않는다(계정-02).
 *
 * <p>구현됨: 이메일 가입·로그인, 카카오·구글 소셜 로그인(인가 코드 서버 교환),
 * 액세스 토큰 발급·검증({@code global.security.JwtProvider}), 리프레시 토큰
 * 발급·회전·무효화({@link RefreshTokenService}), 로그인 실패 잠금, 비밀번호 재설정,
 * 이메일 찾기.
 *
 * <p>미구현: 네이버 공급자(미결-02), 비밀번호 변경(501).
 */
package com.mugunghwa.goodquestion.user.auth;
