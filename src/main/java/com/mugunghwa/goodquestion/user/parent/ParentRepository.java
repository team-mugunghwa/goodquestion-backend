package com.mugunghwa.goodquestion.user.parent;

import com.mugunghwa.goodquestion.user.auth.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ParentRepository extends JpaRepository<Parent, UUID> {

    Optional<Parent> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Parent> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
