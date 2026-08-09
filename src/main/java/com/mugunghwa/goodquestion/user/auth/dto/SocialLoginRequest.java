package com.mugunghwa.goodquestion.user.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 명세 4-1 소셜 로그인 — 서버가 인가 코드를 제공자 토큰으로 교환한다(계정-04). */
public record SocialLoginRequest(
        @NotBlank String authorizationCode,
        @NotBlank String redirectUri
) {
}
