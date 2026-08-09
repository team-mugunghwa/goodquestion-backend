package com.mugunghwa.goodquestion.learning.wordbook.dto;

import com.mugunghwa.goodquestion.learning.wordbook.WordEntryType;
import com.mugunghwa.goodquestion.learning.wordbook.Wordbook;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 명세 3-15 단어. */
public record WordResponse(UUID id, String word, String meaning, String exampleSentence,
                           WordEntryType entryType, UUID sourceSceneId, OffsetDateTime createdAt) {

    public static WordResponse from(Wordbook w) {
        return new WordResponse(w.getId(), w.getWord(), w.getMeaning(), w.getExampleSentence(),
                w.getEntryType(),
                w.getSourceScene() != null ? w.getSourceScene().getId() : null,
                w.getCreatedAt());
    }
}
