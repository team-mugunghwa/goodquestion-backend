package com.mugunghwa.goodquestion.ai.stt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 결과가 어휘 힌트의 반복(에코)인지 판정한다.
 *
 * <p>무음이거나 알아들을 수 없는 소리(샘플레이트 불일치로 뭉개진 녹음 등)가 들어오면
 * 모델이 오디오 대신 prompt의 어휘 힌트에 기대어 출력을 만드는 환각이 있다. 실측된
 * 형태가 셋이다: 힌트 전체를 그대로 반복, 일부만 나열, 힌트 단어들을 조사와 서술어로
 * 이어 문장처럼 재조합("며느리와 시아버지가 방귀를 뀌는 사이, ...").
 *
 * <p>판정은 두 갈래다.
 * <ul>
 *   <li>나열 에코 - 결과에서 힌트 단어를 전부 지웠을 때 아무것도 남지 않으면 힌트 말고는
 *       내용이 없는 것이다. 전체/부분/순서 바뀜을 한 번에 잡는다. 아이가 한 단어로
 *       답하는 경우("방귀!")를 지키기 위해 힌트 단어 2개 이상일 때만 적용한다.</li>
 *   <li>재조합 에코 - 서로 다른 힌트 단어의 등장 비율이 기준 이상이면 에코로 본다.
 *       조사와 서술어가 붙어 나열 판정을 빠져나가는 형태를 잡는다.</li>
 * </ul>
 *
 * <p>오판(정상 발화를 에코로 봄)의 비용은 "다시 말해 볼까?" 안내 한 번이라 크지 않지만,
 * 놓침의 비용은 아이가 하지 않은 말이 저장되고 보호자 리포트 후보에 오르는 것이다.
 *
 * <p>벤더 클라이언트(OpenAiSttClient)가 벤더 원문에 한 번, SpeechService가 어휘 교정
 * (VocabularyCorrector) 뒤에 한 번 더 쓴다 - 뭉개진 에코가 원문 기준 판정을 통과한 뒤
 * 교정으로 정확한 힌트 단어가 되면 놓치는 구멍을 막기 위해 별도 컴포넌트로 뺐다.
 */
@Component
public class VocabularyEchoDetector {

    /**
     * 재조합 에코 판정 기준. 서로 다른 힌트 단어가 이 비율 이상 한 발화에 등장하면
     * 에코로 본다. 힌트는 여러 장면에 걸친 어휘의 합집합이라, 진짜 아이 발화가 이만큼
     * 폭넓게 쓸 일이 없다(실측 재조합 에코는 9개 중 8개, 정상 발화는 2개 수준이었다).
     */
    private static final double ECHO_COVERAGE_THRESHOLD = 2.0 / 3;

    /** 긴 단어부터 지워야 한다 - 짧은 단어가 긴 단어의 일부면 조각이 남는다. */
    private final List<String> hintWords;

    public VocabularyEchoDetector(
            @Value("${external.stt.vocabulary-hint:}") String vocabularyHint) {
        this.hintWords = Arrays.stream(vocabularyHint.split("[,\\s]+"))
                .filter(word -> !word.isBlank())
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
    }

    public boolean isEcho(String text) {
        if (text == null || hintWords.isEmpty()) {
            return false;
        }
        String normalized = normalize(text);
        long matched = hintWords.stream().filter(normalized::contains).count();

        if (matched >= 2) {
            String remainder = normalized;
            for (String word : hintWords) {
                remainder = remainder.replace(word, "");
            }
            if (remainder.isBlank()) {
                return true;
            }
        }
        return hintWords.size() >= 3
                && matched >= Math.ceil(hintWords.size() * ECHO_COVERAGE_THRESHOLD);
    }

    private static String normalize(String value) {
        return value.replaceAll("[\\s,.·!?'\"]", "");
    }
}
