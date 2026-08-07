package com.mugunghwa.goodquestion.user.parent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParentCreateRequest(
        @NotBlank @Size(max = 50) String name
) {
}
