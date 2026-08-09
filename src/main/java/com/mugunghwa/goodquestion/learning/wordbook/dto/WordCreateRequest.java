package com.mugunghwa.goodquestion.learning.wordbook.dto;

import com.mugunghwa.goodquestion.learning.wordbook.WordEntryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** 명세 4-11 단어 저장. meaning·exampleSentence가 없으면 서버가 LLM으로 생성한다(단어-02). */
public record WordCreateRequest(
        @NotBlank @Size(max = 50) String word,
        @NotNull WordEntryType entryType,
        UUID sourceSceneId,
        String meaning,
        String exampleSentence
) {
}
