package com.mugunghwa.goodquestion.user.auth.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 카카오 로그인 어댑터. 클라이언트(모바일 카카오 SDK)가 발급받은 액세스 토큰을
 * 카카오 사용자 정보 API에 그대로 전달해 프로필을 조회한다.
 * 서버 간 인가 코드 교환(client secret)이 필요 없어 해커톤 일정에 맞는 최단 경로.
 */
@Component
@RequiredArgsConstructor
public class DefaultKakaoClient implements KakaoClient {

    private static final String USER_ME_URI = "https://kapi.kakao.com/v2/user/me";

    private final WebClient webClient;

    @Override
    public KakaoProfile getProfile(String accessToken) {
        KakaoUserMeResponse response;
        try {
            response = webClient.get()
                    .uri(USER_ME_URI)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(KakaoUserMeResponse.class)
                    .block();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }

        if (response == null || response.id() == null) {
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }

        String email = response.kakaoAccount() != null ? response.kakaoAccount().email() : null;
        String nickname = response.kakaoAccount() != null && response.kakaoAccount().profile() != null
                ? response.kakaoAccount().profile().nickname()
                : "카카오사용자";

        return new KakaoProfile(String.valueOf(response.id()), email, nickname);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoUserMeResponse(Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoAccount(String email, KakaoProfileField profile) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoProfileField(String nickname) {
    }
}
