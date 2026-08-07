package com.mugunghwa.goodquestion.user.child.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChildCreateRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull @Min(2000) @Max(2100) Short birthYear
) {
}
