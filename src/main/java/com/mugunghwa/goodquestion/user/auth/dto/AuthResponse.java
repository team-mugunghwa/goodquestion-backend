package com.mugunghwa.goodquestion.user.auth.dto;

import com.mugunghwa.goodquestion.user.parent.Parent;
import com.mugunghwa.goodquestion.user.parent.dto.ParentResponse;

/** 명세 4-1 회원가입·로그인 응답 — 토큰 묶음과 보호자 프로필을 함께 돌려준다. */
public record AuthResponse(TokenResponse tokens, ParentResponse parent) {

    public static AuthResponse of(TokenResponse tokens, Parent parent) {
        return new AuthResponse(tokens, ParentResponse.from(parent));
    }
}
