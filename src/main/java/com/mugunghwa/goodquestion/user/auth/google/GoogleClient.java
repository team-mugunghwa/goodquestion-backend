package com.mugunghwa.goodquestion.user.auth.google;

public interface GoogleClient {
    String exchangeCodeForToken(String authorizationCode, String redirectUri);
    GoogleProfile getProfile(String accessToken);
}
