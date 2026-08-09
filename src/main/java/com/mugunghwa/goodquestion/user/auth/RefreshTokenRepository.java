package com.mugunghwa.goodquestion.user.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** 로그아웃·비밀번호 변경 시 해당 보호자의 유효 토큰을 한꺼번에 무효화하기 위해 사용. */
    List<RefreshToken> findAllByParentIdAndRevokedAtIsNull(UUID parentId);
}
