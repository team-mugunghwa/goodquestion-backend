package com.mugunghwa.goodquestion.user.auth.dto;

/**
 * 명세 3-1 토큰 묶음.
 *
 * <p>refreshToken은 아직 null이다 — RefreshToken 엔티티가 없어 회전·무효화를 구현하지 못했다(계정-05).
 * 액세스 토큰 만료 시 재로그인으로 대응한다. TODO: RefreshToken 도입 후 채운다.
 */
public record TokenResponse(String accessToken, String refreshToken, long accessTokenExpiresIn) {

    public static TokenResponse accessOnly(String accessToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, null, expiresInSeconds);
    }
}
