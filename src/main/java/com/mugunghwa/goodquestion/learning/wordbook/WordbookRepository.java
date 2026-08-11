package com.mugunghwa.goodquestion.learning.wordbook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WordbookRepository extends JpaRepository<Wordbook, UUID> {

    boolean existsByChildIdAndWord(UUID childId, String word);

    /** 단어 소유 검증 — 아이가 둘인 보호자가 다른 아이의 단어를 건드리지 못하게 한다. */
    Optional<Wordbook> findByIdAndChildId(UUID id, UUID childId);

    List<Wordbook> findAllByChildIdOrderByCreatedAtDesc(UUID childId);

    List<Wordbook> findAllByChildIdAndEntryTypeOrderByCreatedAtDesc(UUID childId, WordEntryType entryType);
}
