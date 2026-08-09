package com.mugunghwa.goodquestion.story.dialogue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UtteranceAnalysisRepository extends JpaRepository<UtteranceAnalysis, UUID> {

    /** 리포트 생성용 — 세션 전체 분석 결과 */
    @Query("""
            select ua from UtteranceAnalysis ua
            join fetch ua.message m
            where m.session.id = :sessionId
            order by m.turnOrder asc
            """)
    List<UtteranceAnalysis> findAllBySessionId(@Param("sessionId") UUID sessionId);
}
