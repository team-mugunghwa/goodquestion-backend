package com.mugunghwa.goodquestion.learning.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    Optional<Report> findBySessionId(UUID sessionId);

    @Query("select r from Report r where r.session.child.id = :childId order by r.createdAt desc")
    Page<Report> findAllByChildId(@Param("childId") UUID childId, Pageable pageable);
}
