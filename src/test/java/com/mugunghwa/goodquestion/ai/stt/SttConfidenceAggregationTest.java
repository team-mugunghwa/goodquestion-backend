package com.mugunghwa.goodquestion.ai.stt;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 토큰 logprob -> 발화 신뢰도 집계.
 * gpt-4o 계열 transcribe는 요약 통계 없이 토큰 logprob 배열만 주므로
 * 서버 집계(exp(평균) = 토큰 확률의 기하평균)가 맞는지가 판정 전체의 기반이다.
 */
class SttConfidenceAggregationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNode logprobs(String json) {
        return MAPPER.readTree(json);
    }

    @Test
    void 토큰_logprob_평균의_exp를_신뢰도로_계산한다() {
        // 평균 logprob = -0.5 -> exp(-0.5) = 0.6065... -> 0.607
        JsonNode node = logprobs("""
                [{"token": "며느리", "logprob": -0.2},
                 {"token": "가", "logprob": -0.8}]""");

        assertThat(OpenAiSttClient.confidenceFrom(node))
                .isEqualByComparingTo(new BigDecimal("0.607"));
    }

    @Test
    void 확신에_찬_결과는_1에_가깝다() {
        JsonNode node = logprobs("""
                [{"token": "a", "logprob": -0.01}, {"token": "b", "logprob": -0.02}]""");

        assertThat(OpenAiSttClient.confidenceFrom(node))
                .isEqualByComparingTo(new BigDecimal("0.985"));
    }

    /** 벤더가 logprob을 안 주면 모름(null)이다. 0으로 만들면 전부 저신뢰가 돼 버린다. */
    @Test
    void logprob이_없으면_null을_돌려준다() {
        assertThat(OpenAiSttClient.confidenceFrom(null)).isNull();
        assertThat(OpenAiSttClient.confidenceFrom(logprobs("[]"))).isNull();
        assertThat(OpenAiSttClient.confidenceFrom(logprobs("{\"not\": \"array\"}"))).isNull();
        assertThat(OpenAiSttClient.confidenceFrom(logprobs("[{\"token\": \"a\"}]"))).isNull();
    }

    @Test
    void 결과는_0과_1_사이_소수점_3자리다() {
        JsonNode node = logprobs("[{\"token\": \"a\", \"logprob\": 0.0}]");

        assertThat(OpenAiSttClient.confidenceFrom(node))
                .isEqualByComparingTo(BigDecimal.ONE);
    }
}
