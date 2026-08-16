package com.mugunghwa.goodquestion.learning.wordbook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 이야기 어휘 사전 한 줄 - 이야기 단위 단어장(뜻 포함)의 원본.
 *
 * <p>고정 대사의 낱말은 시드로 관리되는 폐집합이라 뜻을 미리 검수해 둘 수 있다.
 * 단어 담기는 뜻 생성 LLM을 부르기 전에 이 사전을 먼저 조회한다
 * ({@link WordbookService}). 시드로만 채워지고 런타임에는 읽기 전용이다.
 *
 * <p>{@code word}는 표제어(조사를 뗀 원형)다 - 조회 키가
 * {@link WordLemmatizer} 정규화 결과이기 때문이다.
 */
@Entity
@Getter
@Table(name = "story_vocabulary")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoryVocabulary {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "story_id", nullable = false)
    private UUID storyId;

    @Column(nullable = false, length = 50)
    private String word;

    @Column(nullable = false, length = 200)
    private String meaning;

    @Column(name = "example_sentence", length = 300)
    private String exampleSentence;
}
