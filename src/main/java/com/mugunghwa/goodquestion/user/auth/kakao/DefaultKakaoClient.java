package com.mugunghwa.goodquestion.user.auth.kakao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 카카오 로그인 어댑터.
 *
 * <p>명세(계정-04)에 따라 서버가 인가 코드를 액세스 토큰으로 교환한 뒤 프로필을 조회한다.
 * 교환에는 카카오 REST API 키가 필요하므로 {@code KAKAO_CLIENT_ID}를 설정해야 한다
 * (미설정 시 401 KAKAO_AUTH_FAILED). client secret은 카카오 콘솔에서 사용함으로
 * 설정한 경우에만 필요하다.
 */
@Component
@RequiredArgsConstructor
public class DefaultKakaoClient implements KakaoClient {

    private static final String TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String USER_ME_URI = "https://kapi.kakao.com/v2/user/me";

    private final WebClient webClient;

    @Value("${kakao.client-id:}")
    private String clientId;

    /** 카카오 콘솔에서 client secret을 사용함으로 설정한 경우에만 값이 있다. */
    @Value("${kakao.client-secret:}")
    private String clientSecret;

    @Override
    public String exchangeCodeForToken(String authorizationCode, String redirectUri) {
        if (!StringUtils.hasText(clientId)) {
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED,
                    "KAKAO_CLIENT_ID가 설정되지 않아 인가 코드를 교환할 수 없습니다.");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", authorizationCode);
        if (StringUtils.hasText(clientSecret)) {
            form.add("client_secret", clientSecret);
        }

        KakaoTokenResponse response;
        try {
            response = webClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(form)
                    .retrieve()
                    .bodyToMono(KakaoTokenResponse.class)
                    .block();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }
        if (response == null || !StringUtils.hasText(response.accessToken())) {
            throw new BusinessException(ErrorCode.KAKAO_AUTH_FAILED);
        }
        return response.accessToken();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

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
