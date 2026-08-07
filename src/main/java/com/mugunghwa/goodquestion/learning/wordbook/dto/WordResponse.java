package com.mugunghwa.goodquestion.learning.wordbook.dto;

import com.mugunghwa.goodquestion.learning.wordbook.Wordbook;

import java.util.UUID;

public record WordResponse(UUID id, String word, String meaning,
                           String exampleSentence, boolean isFavorite) {

    public static WordResponse from(Wordbook w) {
        return new WordResponse(w.getId(), w.getWord(), w.getMeaning(),
                w.getExampleSentence(), w.isFavorite());
    }
}
