package com.mugunghwa.goodquestion.user.parent.dto;

import com.mugunghwa.goodquestion.user.parent.Parent;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ParentResponse(UUID id, String name, OffsetDateTime createdAt) {

    public static ParentResponse from(Parent parent) {
        return new ParentResponse(parent.getId(), parent.getName(), parent.getCreatedAt());
    }
}
