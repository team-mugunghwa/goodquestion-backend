package com.mugunghwa.goodquestion.user.auth.dto;

import com.mugunghwa.goodquestion.user.parent.Parent;
import com.mugunghwa.goodquestion.user.parent.dto.ParentResponse;

/** 소셜 로그인 응답 — 최초 가입 여부를 함께 알려준다. */
public record SocialAuthResponse(TokenResponse tokens, ParentResponse parent, boolean isNewUser) {

    public static SocialAuthResponse of(TokenResponse tokens, Parent parent, boolean isNewUser) {
        return new SocialAuthResponse(tokens, ParentResponse.from(parent), isNewUser);
    }
}