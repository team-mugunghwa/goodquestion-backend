package com.mugunghwa.goodquestion.learning.wordbook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WordbookRepository extends JpaRepository<Wordbook, UUID> {

    boolean existsByChildIdAndWord(UUID childId, String word);

    List<Wordbook> findAllByChildIdOrderByCreatedAtDesc(UUID childId);

    List<Wordbook> findAllByChildIdAndFavoriteOrderByCreatedAtDesc(UUID childId, boolean favorite);
}
