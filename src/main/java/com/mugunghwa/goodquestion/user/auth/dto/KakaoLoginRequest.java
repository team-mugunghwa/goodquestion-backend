package com.mugunghwa.goodquestion.user.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** 클라이언트(모바일 카카오 SDK)가 발급받은 카카오 액세스 토큰을 전달한다. */
public record KakaoLoginRequest(
        @NotBlank String accessToken
) {
}
