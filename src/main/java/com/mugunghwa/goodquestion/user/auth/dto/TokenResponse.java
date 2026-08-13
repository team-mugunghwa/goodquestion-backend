package com.mugunghwa.goodquestion.user.auth.dto;

/**
 * 명세 3-1 토큰 묶음.
 *
 * <p>액세스 토큰(기본 30분)과 리프레시 토큰(기본 14일, 1회 사용 회전)을 함께 담는다.
 * 액세스가 만료되면 {@code POST /api/auth/refresh}로 재발급하고, 리프레시까지 만료되면
 * 재로그인한다(계정-05).
 */
public record TokenResponse(String accessToken, String refreshToken, long accessTokenExpiresIn) {

    /** Access 토큰 단일 전략용. refreshToken은 null로 둔다. */
    public static TokenResponse accessOnly(String accessToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, null, expiresInSeconds);
    }

    //refreshtoken 있는 버전
    public static TokenResponse of(String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, refreshToken, expiresInSeconds);
    }
}
