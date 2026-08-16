package com.mugunghwa.goodquestion.learning.wordbook;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * 예문 따라 말하기 채점 - 목표 예문과 발화 텍스트의 문자 단위 일치율(0.00~1.00).
 *
 * <p>STT가 붙이는 문장부호와 띄어쓰기 차이로 점수가 깎이면 따라 말하기가 받아쓰기 시험이
 * 된다. 글자와 숫자만 남기고 비교하며, 편집 거리(Levenshtein)를 긴 쪽 길이로 나눠 점수를
 * 낸다. 같은 발화는 항상 같은 점수를 받는 결정적 채점이다 - "그대로 따라 말했는가" 판정에
 * 의미 해석이 필요 없어 LLM 채점(지연·비용·비결정성)은 쓰지 않는다.
 */
final class SentenceSimilarity {

    private SentenceSimilarity() {
    }

    /**
     * 소수 둘째 자리로 반올림한 일치율. 지급 기준(0.90) 비교도 이 반올림 값으로 하므로
     * 화면에 보이는 퍼센트와 지급 판정이 어긋나지 않는다.
     */
    static BigDecimal score(String target, String spoken) {
        String normalizedTarget = normalize(target);
        String normalizedSpoken = normalize(spoken);
        if (normalizedTarget.isEmpty() || normalizedSpoken.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        int distance = levenshtein(normalizedTarget, normalizedSpoken);
        int longer = Math.max(normalizedTarget.length(), normalizedSpoken.length());
        return BigDecimal.valueOf(1.0 - (double) distance / longer)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** 글자·숫자만 남긴다. 띄어쓰기·문장부호는 STT 표기 차이일 뿐 발화의 차이가 아니다. */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    /** 표준 두 줄 DP. 예문은 300자 이하라 O(nm)로 충분하다. */
    private static int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int substitution = previous[j - 1]
                        + (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1);
                current[j] = Math.min(substitution, Math.min(previous[j] + 1, current[j - 1] + 1));
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }
}
