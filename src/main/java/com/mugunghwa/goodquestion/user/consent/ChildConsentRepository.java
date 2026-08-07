package com.mugunghwa.goodquestion.user.consent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChildConsentRepository extends JpaRepository<ChildConsent, UUID> {

    boolean existsByChildIdAndWithdrawnAtIsNull(UUID childId);
}
