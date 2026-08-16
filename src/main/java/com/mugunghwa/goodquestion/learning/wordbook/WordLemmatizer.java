package com.mugunghwa.goodquestion.learning.wordbook;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.ko.KoreanTokenizer;
import org.apache.lucene.analysis.ko.POS;
import org.apache.lucene.analysis.ko.tokenattributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 단어장에 담을 단어의 표제어 추출 - "기왓장이" -> "기왓장".
 *
 * <p>아이는 대사 속 어절을 그대로 누르므로 조사가 붙은 채 들어온다. 조사를 떼지 않으면
 * 같은 단어가 "기왓장이"/"기왓장을"로 여러 벌 저장되고, 그때마다 뜻 생성 LLM이
 * 또 호출된다. 표제어가 중복 검사와 이야기 어휘 사전의 키가 되므로 이 정규화가
 * 절감 파이프라인의 맨 앞이다.
 *
 * <p>끝 글자를 조사로 보고 자르는 방식은 쓰지 않는다 - "마을"의 "을", "사과"의 "과"처럼
 * 낱말 일부를 조사로 오인해 단어를 훼손한다. 형태소 분석(Nori, mecab-ko-dic)으로
 * 명사를 식별해서만 뗀다.
 *
 * <p><b>확신이 없으면 원형 그대로 돌려준다.</b> 분석 결과의 머리가 명사가 아니거나
 * 사전에 없는 낱말이면 누른 그대로 저장된다 - 지금까지의 동작과 같아서 더 나빠질
 * 일이 없다.
 */
@Slf4j
@Component
public class WordLemmatizer {

    /**
     * 조사/어미를 뗀 표제어. 뗄 수 없으면(비명사, 미등재어, 분석 실패) 입력 그대로.
     */
    public String lemmatize(String word) {
        if (word == null || word.isBlank()) {
            return word;
        }
        String trimmed = word.trim();
        try {
            List<Morpheme> morphemes = analyze(trimmed);
            String lemma = leadingNouns(morphemes);
            if (lemma.isEmpty() || lemma.equals(trimmed)) {
                return trimmed;
            }
            // 분석기가 낱말을 재조립하다 원문에 없는 글자를 만들면(불규칙 활용 복원 등)
            // 신뢰하지 않는다 - 아이가 누른 글자에서 꼬리만 떼는 것이 목표다.
            if (!trimmed.startsWith(lemma)) {
                return trimmed;
            }
            // 떼어낸 꼬리가 조사/어미일 때만 신뢰한다. 오인식이 만든 엉터리
            // 말("방빙끄")은 머리("방")가 우연히 명사여도 꼬리("빙끄")가
            // 정체불명이다 - 이때 잘라 버리면 엉터리 말이 실제 단어("방")로
            // 둔갑해 유효성 관문(INVALID_WORD)까지 통과한다. 원형 그대로
            // 돌려보내 관문이 원문을 판정하게 한다.
            // (길이 비율로 거르는 방법은 "감에서" -> "감" 같은 정상 사례를
            // 오차단해서 쓰지 않는다)
            if (!hasTrustedTail(morphemes)) {
                return trimmed;
            }
            return lemma;
        } catch (Exception e) {
            log.warn("표제어 분석 실패 - 원형 그대로 저장: word={}", trimmed, e);
            return trimmed;
        }
    }

    /** 머리 명사 뒤의 형태소가 전부 조사/어미/접미 부류인가. */
    private static boolean hasTrustedTail(List<Morpheme> morphemes) {
        boolean inTail = false;
        for (Morpheme morpheme : morphemes) {
            if (!inTail && isNoun(morpheme.pos())) {
                continue;
            }
            inTail = true;
            if (!isFunctionalTail(morpheme.pos())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFunctionalTail(POS.Tag tag) {
        String name = tag.name();
        return name.startsWith("J")    // 조사 (JKS/JKO/JKB/JX/JC ...)
                || name.startsWith("E")  // 어미 (EP/EF/EC/ETN/ETM)
                || tag == POS.Tag.VCP    // 긍정 지정사("이다"의 "이")
                || tag == POS.Tag.XSN;   // 명사 파생 접미사("들" 등)
    }

    /**
     * 어절 머리의 연속된 명사만 이어 붙인다. "기왓장이"(기왓장/NNG + 이/JKS)면
     * "기왓장", "장대가"면 "장대". 머리가 명사가 아니면 빈 문자열 - 동사/부사
     * 어절은 표제어 추출 대상이 아니다(활용형 복원은 오류 여지가 커서 안 한다).
     */
    private static String leadingNouns(List<Morpheme> morphemes) {
        StringBuilder lemma = new StringBuilder();
        for (Morpheme morpheme : morphemes) {
            if (!isNoun(morpheme.pos())) {
                break;
            }
            lemma.append(morpheme.term());
        }
        return lemma.toString();
    }

    private static boolean isNoun(POS.Tag tag) {
        return tag == POS.Tag.NNG   // 일반 명사
                || tag == POS.Tag.NNP  // 고유 명사
                || tag == POS.Tag.NR   // 수사
                || tag == POS.Tag.NP;  // 대명사
    }

    private static List<Morpheme> analyze(String word) throws Exception {
        List<Morpheme> morphemes = new ArrayList<>();
        // 복합 명사를 쪼개지 않는다(NONE) - "기왓장"이 "기와+장"으로 갈라지면
        // 표제어가 사전/중복 키로 못 쓰인다.
        try (KoreanTokenizer tokenizer = new KoreanTokenizer(
                KoreanTokenizer.DEFAULT_TOKEN_ATTRIBUTE_FACTORY,
                null, KoreanTokenizer.DecompoundMode.NONE, false)) {
            tokenizer.setReader(new StringReader(word));
            CharTermAttribute term = tokenizer.addAttribute(CharTermAttribute.class);
            PartOfSpeechAttribute pos = tokenizer.addAttribute(PartOfSpeechAttribute.class);
            tokenizer.reset();
            while (tokenizer.incrementToken()) {
                morphemes.add(new Morpheme(term.toString(), pos.getLeftPOS()));
            }
            tokenizer.end();
        }
        return morphemes;
    }

    private record Morpheme(String term, POS.Tag pos) {
    }
}
