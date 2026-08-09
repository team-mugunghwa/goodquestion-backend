package com.mugunghwa.goodquestion.user.auth.dto;

import com.mugunghwa.goodquestion.user.parent.Parent;

import java.util.UUID;

public record AuthResponse(String accessToken, UUID parentId, String name) {

    public static AuthResponse of(String accessToken, Parent parent) {
        return new AuthResponse(accessToken, parent.getId(), parent.getName());
    }
}
