package com.mugunghwa.goodquestion.learning.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    Optional<Report> findBySessionId(UUID sessionId);

    /**
     * 목록 응답이 이야기 제목을 담으므로 세션과 이야기를 함께 가져온다.
     * 지연 로딩으로 두면 리포트 건수만큼 추가 조회가 붙는다.
     */
    @Query("""
            select r from Report r
            join fetch r.session s
            join fetch s.story
            where s.child.id = :childId
            order by r.createdAt desc
            """)
    List<Report> findAllByChildId(@Param("childId") UUID childId);
}
