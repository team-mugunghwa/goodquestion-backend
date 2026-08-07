package com.mugunghwa.goodquestion.user.consent.dto;

import com.mugunghwa.goodquestion.user.consent.VerificationMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConsentCreateRequest(
        @NotBlank String consentVersion,
        @NotNull VerificationMethod verificationMethod
) {
}
