package com.mugunghwa.goodquestion.learning.wordbook;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WordbookRepository extends JpaRepository<Wordbook, UUID> {

    boolean existsByChildIdAndWord(UUID childId, String word);

    /** 단어 소유 검증 — 아이가 둘인 보호자가 다른 아이의 단어를 건드리지 못하게 한다. */
    Optional<Wordbook> findByIdAndChildId(UUID id, UUID childId);

    /**
     * 목록은 장면·이야기를 함께 가져온다. {@code WordResponse} 가 이야기 제목과 이미지를 담는데,
     * 둘 다 LAZY 라 힌트가 없으면 단어 하나당 쿼리가 두 번씩 더 나간다.
     */
    @EntityGraph(attributePaths = {"sourceScene", "sourceScene.story"})
    List<Wordbook> findAllByChildIdOrderByCreatedAtDesc(UUID childId);

    @EntityGraph(attributePaths = {"sourceScene", "sourceScene.story"})
    List<Wordbook> findAllByChildIdAndEntryTypeOrderByCreatedAtDesc(UUID childId, WordEntryType entryType);
}
