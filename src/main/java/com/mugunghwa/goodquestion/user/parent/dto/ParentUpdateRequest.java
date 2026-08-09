package com.mugunghwa.goodquestion.user.parent.dto;

import jakarta.validation.constraints.Size;

/**
 * 명세 4-2 내 정보 수정 — 전달한 필드만 반영한다.
 * 비밀번호를 바꾸려면 currentPassword가 함께 있어야 한다.
 */
public record ParentUpdateRequest(
        @Size(max = 50) String name,
        String currentPassword,
        @Size(min = 8) String newPassword
) {
}
