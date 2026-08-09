package com.mugunghwa.goodquestion.user.auth.kakao;

public interface KakaoClient {

    /** 카카오 액세스 토큰으로 사용자 프로필을 조회한다. 토큰이 유효하지 않으면 BusinessException(KAKAO_AUTH_FAILED). */
    KakaoProfile getProfile(String accessToken);
}
