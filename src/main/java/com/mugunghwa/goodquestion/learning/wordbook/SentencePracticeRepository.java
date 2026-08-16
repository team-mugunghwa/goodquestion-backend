package com.mugunghwa.goodquestion.learning.wordbook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface SentencePracticeRepository extends JpaRepository<SentencePractice, UUID> {

    /** 예문당 최초 1회 지급 판정. DB unique(wordbook_id, sentence_type)와 함께 이중으로 막는다. */
    boolean existsByWordIdAndSentenceType(UUID wordbookId, ExampleSentenceType sentenceType);

    /** 하루 상한 판정 — 오늘(Asia/Seoul) 보상받은 연습 수 */
    long countByChildIdAndCreatedAtGreaterThanEqual(UUID childId, OffsetDateTime since);
}
