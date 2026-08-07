package com.mugunghwa.goodquestion.user.child.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ChildUpdateRequest(
        @Size(max = 50) String name,
        @Min(2000) @Max(2100) Short birthYear
) {
}
