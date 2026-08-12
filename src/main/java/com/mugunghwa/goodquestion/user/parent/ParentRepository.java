package com.mugunghwa.goodquestion.user.parent;

import com.mugunghwa.goodquestion.user.auth.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParentRepository extends JpaRepository<Parent, UUID> {

    Optional<Parent> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Parent> findByProviderAndProviderId(AuthProvider provider, String providerId);

    /**
     * 이메일 찾기(계정-07) — 이름만으로 찾는 경로용. 자녀가 하나라도 등록된 계정은 절대
     * 포함하지 않는다. 아이 정보 없이는 자녀가 있는 계정을 찾을 수 없게 막는 것이 핵심이다.
     */
    @Query("select p from Parent p where p.name = :name "
            + "and not exists (select 1 from Child c where c.parent = p)")
    List<Parent> findByNameAndNoChildren(@Param("name") String name);
}
