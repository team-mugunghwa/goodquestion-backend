package com.mugunghwa.goodquestion.ai.word;

import com.mugunghwa.goodquestion.ai.llm.LlmClient;
import com.mugunghwa.goodquestion.ai.word.WordMeaningLlmClient.WordMeaningResult;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * WordMeaningLlmClient 단위 테스트.
 *
 * <p>{@link LlmClient}는 인터페이스라 Mockito로 바로 mock한다 — HTTP 계층을 스텁할 필요가
 * 없다. (이 프로젝트에는 AnalysisLlmClient/CharacterLlmClient에 대한 테스트가 없어
 * 참고할 정해진 mock 패턴이 없었다.)
 */
class WordMeaningLlmClientTest {

    private final WordMeaningPromptBuilder promptBuilder = new WordMeaningPromptBuilder();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private WordMeaningLlmClient client(LlmClient llmClient) {
        return new WordMeaningLlmClient(llmClient, promptBuilder);
    }

    private LlmClient.LlmJsonResult resultOf(String meaning, String exampleSentence) {
        ObjectNode node = objectMapper.createObjectNode();
        if (meaning != null) {
            node.put("meaning", meaning);
        }
        if (exampleSentence != null) {
            node.put("exampleSentence", exampleSentence);
        }
        return new LlmClient.LlmJsonResult(node, "gpt-5-mini");
    }

    @Test
    void 정상_응답이면_meaning과_예문을_그대로_돌려준다() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.completeJson(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(resultOf("참나무 열매", "다람쥐가 도토리를 모으고 있어요."));

        WordMeaningResult result = client(llmClient).generate("도토리", "다람쥐가 도토리를 모으고 있다");

        assertThat(result.meaning()).isEqualTo("참나무 열매");
        assertThat(result.exampleSentence()).isEqualTo("다람쥐가 도토리를 모으고 있어요.");
    }

    @Test
    void LlmClient가_예외를_던지면_전파하지_않고_대체_문구를_돌려준다() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.completeJson(anyString(), anyString(), anyString(), anyMap()))
                .thenThrow(new IllegalStateException("LLM 응답이 비어 있습니다"));

        WordMeaningResult result = client(llmClient).generate("도토리", null);

        assertThat(result.meaning()).isEqualTo("지금은 뜻을 알려줄 수 없어요");
        assertThat(result.exampleSentence()).isNull();
    }

    @Test
    void meaning이_없는_응답이면_대체_문구를_돌려준다() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.completeJson(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(resultOf(null, "다람쥐가 도토리를 모으고 있어요."));

        WordMeaningResult result = client(llmClient).generate("도토리", null);

        assertThat(result.meaning()).isEqualTo("지금은 뜻을 알려줄 수 없어요");
        assertThat(result.exampleSentence()).isNull();
    }

    @Test
    void meaning이_빈_문자열이면_대체_문구를_돌려준다() {
        LlmClient llmClient = mock(LlmClient.class);
        when(llmClient.completeJson(anyString(), anyString(), anyString(), anyMap()))
                .thenReturn(resultOf("", "다람쥐가 도토리를 모으고 있어요."));

        WordMeaningResult result = client(llmClient).generate("도토리", null);

        assertThat(result.meaning()).isEqualTo("지금은 뜻을 알려줄 수 없어요");
        assertThat(result.exampleSentence()).isNull();
    }
}
