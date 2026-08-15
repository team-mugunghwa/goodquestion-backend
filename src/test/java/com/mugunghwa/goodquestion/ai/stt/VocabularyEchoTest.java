package com.mugunghwa.goodquestion.ai.stt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 어휘 힌트 에코 판정.
 *
 * 무음이나 뭉개진 오디오가 들어오면 모델이 prompt의 어휘 힌트에 기대어 출력을 만든다.
 * 실측된 형태 셋(전체 반복, 일부 나열, 문장형 재조합)을 모두 잡되, 힌트 단어가 들어간
 * 정상 발화를 에코로 오판하지 않는지가 반대쪽 경계다 - 오판이 잦으면 제대로 말한
 * 아이에게 자꾸 다시 말하라고 하게 된다.
 */
class VocabularyEchoTest {

    private static final String HINT = "며느리, 시아버지, 방귀, 친정, 갓, 이장, 배나무, 장대, 기왓장";

    private OpenAiSttClient client(String hint) {
        return new OpenAiSttClient(null, "http://unused", "unused-key", "unused-model", hint);
    }

    @Test
    void 힌트_전체를_그대로_반복하면_에코다() {
        assertThat(client(HINT).isVocabularyEcho(
                "며느리, 시아버지, 방귀, 친정, 갓, 이장, 배나무, 장대, 기왓장")).isTrue();
    }

    @Test
    void 순서가_바뀌거나_구두점이_달라도_에코다() {
        assertThat(client(HINT).isVocabularyEcho(
                "방귀! 며느리. 기왓장 장대 배나무 이장 갓 친정 시아버지")).isTrue();
    }

    @Test
    void 힌트_일부만_나열해도_에코다() {
        assertThat(client(HINT).isVocabularyEcho("며느리, 시아버지, 방귀")).isTrue();
        assertThat(client(HINT).isVocabularyEcho("배나무 장대")).isTrue();
    }

    /** 실측 사례. 조사와 서술어를 붙여 문장처럼 재조합해도 힌트 어휘 대부분이 등장한다. */
    @Test
    void 힌트_단어를_문장으로_재조합해도_에코다() {
        assertThat(client(HINT).isVocabularyEcho(
                "며느리와 시아버지가 방귀를 뀌는 사이, 친정에서 갓을 쓴 이장이 배나무를 잘라 기왓장을 쌓았다."))
                .isTrue();
    }

    @Test
    void 힌트_단어가_들어간_정상_발화는_에코가_아니다() {
        assertThat(client(HINT).isVocabularyEcho("며느리가 방귀를 뀌어서 배가 떨어졌어요")).isFalse();
        assertThat(client(HINT).isVocabularyEcho("시아버지한테 솔직하게 말하는 게 좋을 것 같아요")).isFalse();
    }

    /** 아이가 한 단어로 답하는 것은 정상이다. 나열 판정은 힌트 단어 2개부터만 적용한다. */
    @Test
    void 힌트_단어_하나만_말한_것은_에코가_아니다() {
        assertThat(client(HINT).isVocabularyEcho("방귀!")).isFalse();
        assertThat(client(HINT).isVocabularyEcho("며느리")).isFalse();
    }

    @Test
    void 힌트_단어_두어_개에_실제_내용이_붙으면_에코가_아니다() {
        assertThat(client(HINT).isVocabularyEcho("방귀 뀌는 며느리가 계속 참았어요")).isFalse();
    }

    @Test
    void 힌트가_없거나_결과가_null이면_판정하지_않는다() {
        assertThat(client("").isVocabularyEcho("며느리, 시아버지, 방귀")).isFalse();
        assertThat(client(HINT).isVocabularyEcho(null)).isFalse();
    }
}
