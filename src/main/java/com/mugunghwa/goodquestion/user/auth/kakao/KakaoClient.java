package com.mugunghwa.goodquestion.user.auth.kakao;

public interface KakaoClient {

    /**
     * 인가 코드를 카카오 액세스 토큰으로 교환한다(계정-04 서버 교환).
     *
     * @return 카카오 액세스 토큰 — 실패 시 BusinessException(KAKAO_AUTH_FAILED)
     */
    String exchangeCodeForToken(String authorizationCode, String redirectUri);

    /** 카카오 액세스 토큰으로 사용자 프로필을 조회한다. 토큰이 유효하지 않으면 BusinessException(KAKAO_AUTH_FAILED). */
    KakaoProfile getProfile(String accessToken);
}
