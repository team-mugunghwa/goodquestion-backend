package com.mugunghwa.goodquestion.user.consent.dto;

import com.mugunghwa.goodquestion.user.consent.ChildConsent;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsentResponse(UUID id, String consentVersion,
                              OffsetDateTime consentedAt, OffsetDateTime withdrawnAt) {

    public static ConsentResponse from(ChildConsent consent) {
        return new ConsentResponse(consent.getId(), consent.getConsentVersion(),
                consent.getConsentedAt(), consent.getWithdrawnAt());
    }
}
