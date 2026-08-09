package com.mugunghwa.goodquestion.user.parent.dto;

import com.mugunghwa.goodquestion.user.auth.AuthProvider;
import com.mugunghwa.goodquestion.user.parent.Parent;

import java.util.UUID;

/**
 * 명세 3-2 보호자 프로필 — 인증 응답과 내 정보 조회·수정이 공유한다.
 * provider는 소셜 계정일 때만 값이 있고 이메일 계정은 null이다.
 */
public record ParentResponse(UUID id, String email, String name, AuthProvider provider) {

    public static ParentResponse from(Parent parent) {
        AuthProvider provider = parent.isLocal() ? null : parent.getProvider();
        return new ParentResponse(parent.getId(), parent.getEmail(), parent.getName(), provider);
    }
}
