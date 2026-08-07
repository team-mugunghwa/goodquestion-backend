package com.mugunghwa.goodquestion.learning.wordbook;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WordbookRepository extends JpaRepository<Wordbook, UUID> {

    boolean existsByChildIdAndWord(UUID childId, String word);

    Page<Wordbook> findAllByChildId(UUID childId, Pageable pageable);

    Page<Wordbook> findAllByChildIdAndFavoriteTrue(UUID childId, Pageable pageable);
}
