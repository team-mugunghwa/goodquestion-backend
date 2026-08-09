package com.mugunghwa.goodquestion.user.auth.kakao;

/** 카카오 사용자 프로필 (필요한 필드만 매핑) */
public record KakaoProfile(String providerId, String email, String nickname) {
}
