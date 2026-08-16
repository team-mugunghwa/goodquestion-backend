package com.mugunghwa.goodquestion.learning.wordbook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 표제어 추출.
 *
 * <p>핵심은 두 방향이다 - 조사는 떼되("기왓장이" -> "기왓장"), 낱말 일부를
 * 조사로 오인해 훼손하지 않는다("마을"이 "마"가 되면 안 된다).
 */
class WordLemmatizerTest {

    private final WordLemmatizer lemmatizer = new WordLemmatizer();

    @Test
    void 조사를_뗀다() {
        assertThat(lemmatizer.lemmatize("기왓장이")).isEqualTo("기왓장");
        assertThat(lemmatizer.lemmatize("장대가")).isEqualTo("장대");
        assertThat(lemmatizer.lemmatize("며느리를")).isEqualTo("며느리");
        assertThat(lemmatizer.lemmatize("배나무에서")).isEqualTo("배나무");
        assertThat(lemmatizer.lemmatize("방귀도")).isEqualTo("방귀");
    }

    @Test
    void 조사처럼_보이는_낱말_일부는_떼지_않는다() {
        assertThat(lemmatizer.lemmatize("마을")).isEqualTo("마을");
        assertThat(lemmatizer.lemmatize("사과")).isEqualTo("사과");
        assertThat(lemmatizer.lemmatize("이장")).isEqualTo("이장");
        assertThat(lemmatizer.lemmatize("장대")).isEqualTo("장대");
    }

    @Test
    void 이야기_고전_어휘가_사전에_있다() {
        // mecab-ko-dic에 우리 이야기 어휘가 등재돼 있는지 - 없으면 사용자
        // 사전 추가가 필요하므로 테스트로 못을 박아 둔다.
        assertThat(lemmatizer.lemmatize("곶감을")).isEqualTo("곶감");
        assertThat(lemmatizer.lemmatize("시아버지가")).isEqualTo("시아버지");
        assertThat(lemmatizer.lemmatize("기왓장")).isEqualTo("기왓장");
    }

    @Test
    void 명사가_아니면_원형_그대로다() {
        // 동사/형용사 활용형 복원은 오류 여지가 커서 하지 않는다.
        assertThat(lemmatizer.lemmatize("들썩이고")).isEqualTo("들썩이고");
        assertThat(lemmatizer.lemmatize("어떻게")).isEqualTo("어떻게");
    }

    @Test
    void null과_빈_문자열은_그대로_돌려준다() {
        assertThat(lemmatizer.lemmatize(null)).isNull();
        assertThat(lemmatizer.lemmatize("")).isEmpty();
        assertThat(lemmatizer.lemmatize("  기왓장이  ")).isEqualTo("기왓장");
    }
}
