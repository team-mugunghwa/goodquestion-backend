package com.mugunghwa.goodquestion.user.auth.dto;

/**
 * 명세 3-1 토큰 묶음. 재발급 시 리프레시 토큰을 회전시킨다(계정-05).
 * TODO: RefreshToken 엔티티가 없어 아직 refreshToken을 발급하지 못한다.
 */
public record TokenResponse(String accessToken, String refreshToken, long accessTokenExpiresIn) {
}
