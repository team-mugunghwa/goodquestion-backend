package com.mugunghwa.goodquestion.ai.stt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 이야기 어휘 근접 오인식 교정 — "방비"를 "방귀"로.
 *
 * <p>아동 발음에서 이야기 고유 어휘가 비슷한 소리의 다른 단어로 인식되는 사고가 실측됐다
 * ("방귀"→"방비"). 어휘가 9단어 폐집합(vocabulary-hint)이라, LLM 없이 자모 편집거리만으로
 * 정밀하게 잡을 수 있다 — 비용 0원에 결정론적이다.
 *
 * <p>과교정 안전장치 둘: <b>초성 일치</b>(첫 자모가 다르면 다른 단어를 말했을 가능성이
 * 높다)와 <b>거리 상한</b>(자모 길이의 1/3). 이미 사전 단어와 일치하는 구간은 건드리지
 * 않는다.
 *
 * <p>교정본은 화면·판정용이고 <b>원문은 rawText로 보존</b>된다({@code TranscriptionResponse})
 * — 교정이 틀렸을 때 무엇이 실제로 인식됐는지 추적할 수 있어야 한다.
 */
@Component
public class VocabularyCorrector {

    /** 호환 자모 초성. */
    private static final char[] CHOSEONG = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ".toCharArray();

    /** 호환 자모 중성. 복모음은 아래 EXPAND에서 단모음 둘로 편다. */
    private static final char[] JUNGSEONG = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ".toCharArray();

    /** 호환 자모 종성(0 = 받침 없음). */
    private static final String[] JONGSEONG = {
            "", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ",
            "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ",
            "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ",
    };

    private final List<String> vocabulary;

    public VocabularyCorrector(
            @Value("${external.stt.vocabulary-hint:}") String vocabularyHint) {
        this.vocabulary = Arrays.stream(vocabularyHint.split(","))
                .map(String::trim)
                .filter(word -> !word.isEmpty())
                .toList();
    }

    /**
     * 사전 단어와 자모 거리가 가까운 구간을 사전 단어로 치환한다.
     * 사전이 비었거나 고칠 것이 없으면 원문 그대로.
     */
    public String correct(String text) {
        if (text == null || text.isEmpty() || vocabulary.isEmpty()) {
            return text;
        }
        String corrected = text;
        for (String word : vocabulary) {
            corrected = correctWord(corrected, word);
        }
        return corrected;
    }

    private String correctWord(String text, String word) {
        // 1음절 단어는 퍼지 매칭 신호가 부족하다 - 거리 1이면 음절 공간의 절반이
        // 잡혀서, "갓"이 조사 "가"를 전부 "갓"으로 바꿔치는 과교정이 실제로 났다.
        // 짧은 단어는 정확히 말했을 때만(위 contains) 인정한다.
        if (word.length() < 2) {
            return text;
        }
        // 이미 들어 있으면 손대지 않는다 - 올바른 인식을 재치환할 이유가 없다
        if (text.contains(word)) {
            return text;
        }
        String wordJamo = toJamo(word);
        int maxDistance = Math.max(1, wordJamo.length() / 3);
        int window = word.length();

        StringBuilder out = new StringBuilder(text);
        for (int i = 0; i + window <= out.length(); i++) {
            String candidate = out.substring(i, i + window);
            if (!isAllHangul(candidate)) {
                continue;
            }
            String candidateJamo = toJamo(candidate);
            // 초성 불일치는 다른 단어일 가능성이 높다 - 과교정 방지
            if (candidateJamo.isEmpty() || wordJamo.isEmpty()
                    || candidateJamo.charAt(0) != wordJamo.charAt(0)) {
                continue;
            }
            int distance = levenshtein(candidateJamo, wordJamo);
            if (distance > 0 && distance <= maxDistance) {
                out.replace(i, i + window, word);
                i += word.length() - 1;
            }
        }
        return out.toString();
    }

    private static boolean isAllHangul(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 0xAC00 || c > 0xD7A3) {
                return false;
            }
        }
        return true;
    }

    /** 음절을 호환 자모 나열로 편다. "방귀" → ㅂㅏㅇㄱㅜㅣ (ㅟ는 ㅜㅣ로 확장). */
    static String toJamo(String text) {
        StringBuilder jamo = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < 0xAC00 || c > 0xD7A3) {
                jamo.append(c);
                continue;
            }
            int syllable = c - 0xAC00;
            jamo.append(CHOSEONG[syllable / (21 * 28)]);
            jamo.append(expandVowel(JUNGSEONG[(syllable / 28) % 21]));
            jamo.append(JONGSEONG[syllable % 28]);
        }
        return jamo.toString();
    }

    /** 복모음을 단모음 둘로 편다 - "귀(ㅟ)"와 "기(ㅣ)"의 거리가 1이 되게. */
    private static String expandVowel(char vowel) {
        return switch (vowel) {
            case 'ㅘ' -> "ㅗㅏ";
            case 'ㅙ' -> "ㅗㅐ";
            case 'ㅚ' -> "ㅗㅣ";
            case 'ㅝ' -> "ㅜㅓ";
            case 'ㅞ' -> "ㅜㅔ";
            case 'ㅟ' -> "ㅜㅣ";
            case 'ㅢ' -> "ㅡㅣ";
            default -> String.valueOf(vowel);
        };
    }

    private static int levenshtein(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int substitution = previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(Math.min(previous[j] + 1, current[j - 1] + 1), substitution);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
