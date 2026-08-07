package com.mugunghwa.goodquestion.learning.activity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PostActivityResultRepository extends JpaRepository<PostActivityResult, UUID> {

    Optional<PostActivityResult> findBySessionId(UUID sessionId);
}
