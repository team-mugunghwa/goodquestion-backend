package com.mugunghwa.goodquestion.user.auth.dto;

/**
 * 명세 3-1 토큰 묶음.
 *
 * <p>MVP는 <b>Access 토큰 단일 전략</b>이다. 만료(기본 7일)되면 재로그인으로 재발급한다.
 * 따라서 {@code refreshToken}은 항상 null이고, 클라이언트는 이 필드를 읽지 않아야 한다.
 *
 * <p>회전·무효화에 필요한 {@code refresh_tokens} 테이블과 {@link
 * com.mugunghwa.goodquestion.user.auth.RefreshToken} 엔티티는 이미 있다 — 남은 것은
 * 발급·회전·무효화 로직뿐이다(계정-05). 구현 전까지 {@code POST /api/auth/refresh}와
 * {@code /logout}은 501을 돌려준다.
 *
 * <p>도입 시 이 record는 그대로 두고 {@link #accessOnly} 대신 두 토큰을 채운 생성자를 쓰면 된다 —
 * 응답 스키마가 바뀌지 않으므로 클라이언트 변경이 필요 없다.
 */
public record TokenResponse(String accessToken, String refreshToken, long accessTokenExpiresIn) {

    /** Access 토큰 단일 전략용. refreshToken은 null로 둔다. */
    public static TokenResponse accessOnly(String accessToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, null, expiresInSeconds);
    }
}
