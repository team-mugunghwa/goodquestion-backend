package com.mugunghwa.goodquestion.user.auth.google;

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

@Component
@RequiredArgsConstructor
public class DefaultGoogleClient implements GoogleClient {

    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URI = "https://openidconnect.googleapis.com/v1/userinfo";

    private final WebClient webClient;

    @Value("${google.client-id:}")
    private String clientId;

    @Value("${google.client-secret:}")
    private String clientSecret;

    @Override
    public String exchangeCodeForToken(String authorizationCode, String redirectUri) {
        if (!StringUtils.hasText(clientId)) {
            throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED,
                    "GOOGLE_CLIENT_ID가 필요합니다.");
        }
        if (!StringUtils.hasText(clientSecret)) {
            throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED,
                    "GOOGLE_CLIENT_SECRET이 필요합니다.");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("code", authorizationCode);

        GoogleTokenResponse response;
        try {
            response = webClient.post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(form)
                    .retrieve()
                    .bodyToMono(GoogleTokenResponse.class)
                    .block();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED);
        }
        if (response == null || !StringUtils.hasText(response.accessToken())) {
            throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED);
        }
        return response.accessToken();
    }

    @Override
    public GoogleProfile getProfile(String accessToken) {
        GoogleUserInfo response;
        try {
            response = webClient.get()
                    .uri(USER_INFO_URI)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(GoogleUserInfo.class)
                    .block();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED);
        }
        if (response == null || !StringUtils.hasText(response.subject())) {
            throw new BusinessException(ErrorCode.GOOGLE_AUTH_FAILED);
        }
        String name = StringUtils.hasText(response.name()) ? response.name() : "구글 사용자";
        return new GoogleProfile(response.subject(), response.email(), name);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleUserInfo(@JsonProperty("sub") String subject, String email, String name) {
    }
}
