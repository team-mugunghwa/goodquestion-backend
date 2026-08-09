package com.mugunghwa.goodquestion.user.consent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChildConsentRepository extends JpaRepository<ChildConsent, UUID> {

    boolean existsByChildIdAndWithdrawnAtIsNull(UUID childId);

    /** 철회 이력만 있는 경우와 아예 동의한 적 없는 경우를 구분하기 위해 필요하다. */
    boolean existsByChildId(UUID childId);

    Optional<ChildConsent> findFirstByChildIdAndWithdrawnAtIsNullOrderByConsentedAtDesc(UUID childId);

    List<ChildConsent> findAllByChildIdOrderByConsentedAtDesc(UUID childId);
}
