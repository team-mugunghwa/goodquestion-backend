package com.mugunghwa.goodquestion.learning.wordbook.dto;

import com.mugunghwa.goodquestion.learning.wordbook.ExampleSentenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param sentenceType 따라 말한 예문 유형. 목표 문장은 서버가 단어에서 꺼낸다 —
 *                     문장을 받으면 쉬운 문장으로 바꿔치기해 보상을 딸 수 있다.
 * @param spokenText   /api/stt가 돌려준 인식 텍스트. 음성 자체는 보내지도 저장하지도 않는다.
 */
public record SentencePracticeRequest(
        @NotNull ExampleSentenceType sentenceType,
        @NotBlank @Size(max = 500) String spokenText
) {
}
