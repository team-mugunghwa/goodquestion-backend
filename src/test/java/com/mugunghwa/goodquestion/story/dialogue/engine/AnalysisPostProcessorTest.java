package com.mugunghwa.goodquestion.story.dialogue.engine;

import com.mugunghwa.goodquestion.global.vocab.ThinkingElement;
import com.mugunghwa.goodquestion.story.dialogue.DetectedElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 분석 후처리의 단위 테스트.
 *
 * <p>여기서 걸러내지 못한 요소는 그대로 누적 상태에 들어가 장면 종료 판정까지 흔든다.
 */
class AnalysisPostProcessorTest {

    private final AnalysisPostProcessor postProcessor = new AnalysisPostProcessor();

    @Test
    void 발화에_없는_근거는_버린다() {
        AnalysisPostProcessor.Result result = postProcessor.process(
                List.of(new DetectedElement(ThinkingElement.REASON, "사다리를 놓으면 돼요")),
                "며느리가 방귀를 뀌면 배가 떨어져요");

        assertThat(result.accepted()).isEmpty();
        assertThat(result.dropped()).hasSize(1);
    }

    @Test
    void 띄어쓰기나_문장부호가_달라도_근거로_인정한다() {
        AnalysisPostProcessor.Result result = postProcessor.process(
                List.of(new DetectedElement(ThinkingElement.REASON, "방귀가 세니까")),
                "며느리 방귀가 세니까 배가 떨어질 거예요.");

        assertThat(result.accepted()).hasSize(1);
    }

    @Test
    void 같은_요소가_두_번_잡히면_첫_근거만_남긴다() {
        AnalysisPostProcessor.Result result = postProcessor.process(
                List.of(new DetectedElement(ThinkingElement.REASON, "방귀가 세니까"),
                        new DetectedElement(ThinkingElement.REASON, "배가 떨어질")),
                "며느리 방귀가 세니까 배가 떨어질 거예요");

        assertThat(result.accepted()).hasSize(1);
        assertThat(result.accepted().getFirst().evidence()).isEqualTo("방귀가 세니까");
        assertThat(result.dropped()).hasSize(1);
    }

    @Test
    void 앞선_요소가_무효여도_같은_종류의_유효한_요소는_살린다() {
        AnalysisPostProcessor.Result result = postProcessor.process(
                List.of(new DetectedElement(ThinkingElement.REASON, "지어낸 근거"),
                        new DetectedElement(ThinkingElement.REASON, "방귀가 세니까")),
                "며느리 방귀가 세니까 배가 떨어져요");

        assertThat(result.accepted()).hasSize(1);
        assertThat(result.accepted().getFirst().evidence()).isEqualTo("방귀가 세니까");
    }

    @Test
    void 방법이_없는_당위_표현은_해결책으로_세지_않는다() {
        AnalysisPostProcessor.Result result = postProcessor.process(
                List.of(new DetectedElement(ThinkingElement.SOLUTION, "잘하면 돼요")),
                "그냥 잘하면 돼요");

        assertThat(result.accepted()).isEmpty();
        assertThat(result.dropped()).hasSize(1);
    }

    @Test
    void 구체적인_해결책은_통과시킨다() {
        AnalysisPostProcessor.Result result = postProcessor.process(
                List.of(new DetectedElement(ThinkingElement.SOLUTION, "며느리가 나무 앞에서 방귀를 뀌어요")),
                "며느리가 나무 앞에서 방귀를 뀌어요");

        assertThat(result.accepted()).hasSize(1);
        assertThat(result.acceptedElementNames()).containsExactly("SOLUTION");
    }
}
