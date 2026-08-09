package com.mugunghwa.goodquestion.learning.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    Optional<Report> findBySessionId(UUID sessionId);

    @Query("select r from Report r where r.session.child.id = :childId order by r.createdAt desc")
    List<Report> findAllByChildId(@Param("childId") UUID childId);
}
