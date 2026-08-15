package com.mugunghwa.goodquestion.ai.stt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이야기 어휘 근접 오인식 교정.
 *
 * <p>실측 사고("방귀"→"방비")를 기준 사례로 삼는다. 과교정이 더 무섭다 —
 * 아이가 실제로 다른 말을 했는데 이야기 어휘로 바꿔치면 판정이 오염된다.
 */
class VocabularyCorrectorTest {

    private final VocabularyCorrector corrector = new VocabularyCorrector(
            "며느리, 시아버지, 방귀, 친정, 갓, 이장, 배나무, 장대, 기왓장");

    @Test
    void 실측_사고_방비를_방귀로_교정한다() {
        // ㅂㅏㅇㄱㅜㅣ vs ㅂㅏㅇㅂㅣ - 거리 2, 상한 2(6/3), 초성 ㅂ 일치
        assertThat(corrector.correct("방비를 뀌면 안 돼요")).isEqualTo("방귀를 뀌면 안 돼요");
    }

    @Test
    void 이미_맞게_인식된_단어는_건드리지_않는다() {
        String text = "방귀를 참으면 배가 아파요";
        assertThat(corrector.correct(text)).isEqualTo(text);
    }

    @Test
    void 초성이_다르면_거리가_가까워도_교정하지_않는다() {
        // "강귀" - 방귀와 거리 1이지만 초성 ㄱ != ㅂ. 다른 단어를 말했을 가능성이 높다
        assertThat(corrector.correct("강귀")).isEqualTo("강귀");
    }

    @Test
    void 거리가_먼_단어는_교정하지_않는다() {
        assertThat(corrector.correct("바나나를 먹었어요")).isEqualTo("바나나를 먹었어요");
    }

    @Test
    void 사전에_없는_일상어는_그대로_둔다() {
        String text = "친구가 놀렸어요";
        assertThat(corrector.correct(text)).isEqualTo(text);
    }

    @Test
    void 사전이_비어_있으면_원문_그대로다() {
        VocabularyCorrector empty = new VocabularyCorrector("");
        assertThat(empty.correct("방비")).isEqualTo("방비");
    }

    @Test
    void 복모음은_단모음으로_펴서_잰다() {
        // "방기" (ㅂㅏㅇㄱㅣ) vs "방귀" (ㅂㅏㅇㄱㅜㅣ) - ㅟ를 ㅜㅣ로 펴면 거리 1
        assertThat(corrector.correct("방기 소리가 났어요")).isEqualTo("방귀 소리가 났어요");
    }

    @Test
    void null과_빈_문자열은_그대로_돌려준다() {
        assertThat(corrector.correct(null)).isNull();
        assertThat(corrector.correct("")).isEmpty();
    }
}
