package com.mugunghwa.goodquestion.learning.wordbook;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWalletRepository;
import com.mugunghwa.goodquestion.learning.wordbook.dto.SentencePracticeResponse;
import com.mugunghwa.goodquestion.learning.wordbook.dto.SentencePracticeResponse.SkipReason;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.child.ChildService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 예문 따라 말하기 — 일치율 90% 이상 · 예문(단어 x 유형)당 최초 1회 · 2개 · 하루 최대 2건.
 *
 * <p>지우(잔액 14)로 검증한다. 예문 3종을 직접 넣어 저장해 LLM 호출 없이 준비한다.
 */
@IntegrationTest
@Transactional
class SentencePracticeServiceTest {

    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");
    private static final UUID SIBLING_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000002");

    private static final String STORY_SENTENCE = "가마솥에 누룽지가 눌었어요";
    private static final String DAILY_SENTENCE = "밥을 먹고 나니 배가 불러요";

    @Autowired
    private SentencePracticeService practiceService;

    @Autowired
    private WordbookRepository wordbookRepository;

    @Autowired
    private StardustWalletRepository walletRepository;

    @Autowired
    private ChildService childService;

    /** 예문 3종을 직접 넣어 저장한다 — 생성 경로의 LLM을 타지 않기 위해서다. */
    private UUID newWord(String word) {
        Child child = childService.getOwnedChild(PARENT_ID, CHILD_ID);
        return wordbookRepository.save(Wordbook.builder()
                .child(child).word(word).meaning("테스트용 뜻")
                .exampleSentence(STORY_SENTENCE)
                .exampleDaily(DAILY_SENTENCE)
                .exampleAdvanced(null)
                .entryType(WordEntryType.UNKNOWN)
                .build()).getId();
    }

    private int balance() {
        return walletRepository.findByChildId(CHILD_ID).orElseThrow().getBalance();
    }

    @Test
    void 예문을_그대로_따라_말하면_별가루_2개가_들어온다() {
        UUID wordId = newWord("가마솥");
        int before = balance();

        SentencePracticeResponse response = practiceService.practice(
                PARENT_ID, CHILD_ID, wordId, ExampleSentenceType.STORY, STORY_SENTENCE);

        assertThat(response.matched()).isTrue();
        assertThat(response.similarity()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(response.targetSentence()).isEqualTo(STORY_SENTENCE);
        assertThat(response.rewarded()).isTrue();
        assertThat(response.stardustAmount()).isEqualTo(2);
        assertThat(response.stardustBalance()).isEqualTo(before + 2);
    }

    /** 띄어쓰기·문장부호 차이는 STT 표기일 뿐이다 — 표기 차이로 떨어뜨리면 연습이 시험이 된다. */
    @Test
    void 띄어쓰기와_문장부호가_달라도_인정한다() {
        UUID wordId = newWord("누룽지");

        SentencePracticeResponse response = practiceService.practice(
                PARENT_ID, CHILD_ID, wordId, ExampleSentenceType.STORY, "가마 솥에 누룽지가 눌었어요!");

        assertThat(response.matched()).isTrue();
        assertThat(response.rewarded()).isTrue();
    }

    @Test
    void 일치율이_기준_미만이면_보상도_기록도_없다() {
        UUID wordId = newWord("배나무");
        int before = balance();

        SentencePracticeResponse response = practiceService.practice(
                PARENT_ID, CHILD_ID, wordId, ExampleSentenceType.STORY, "오늘 날씨가 좋아요");

        assertThat(response.matched()).isFalse();
        assertThat(response.rewarded()).isFalse();
        assertThat(response.similarity()).isLessThan(new BigDecimal("0.90"));
        assertThat(response.skipReason()).isNull();
        assertThat(response.stardustAmount()).isZero();
        assertThat(balance()).isEqualTo(before);

        // 기록이 없으므로 같은 예문으로 다시 성공하면 그때 보상받는다
        assertThat(practiceService.practice(PARENT_ID, CHILD_ID, wordId,
                ExampleSentenceType.STORY, STORY_SENTENCE).rewarded()).isTrue();
    }

    @Test
    void 같은_예문은_한_번만_보상한다() {
        UUID wordId = newWord("부지깽이");
        practiceService.practice(PARENT_ID, CHILD_ID, wordId, ExampleSentenceType.STORY, STORY_SENTENCE);
        int after = balance();

        SentencePracticeResponse second = practiceService.practice(
                PARENT_ID, CHILD_ID, wordId, ExampleSentenceType.STORY, STORY_SENTENCE);

        assertThat(second.matched()).isTrue();       // 연습 자체는 언제든 환영
        assertThat(second.rewarded()).isFalse();
        assertThat(second.skipReason()).isEqualTo(SkipReason.ALREADY_REWARDED);
        assertThat(second.stardustAmount()).isZero();
        assertThat(balance()).isEqualTo(after);
    }

    /** 유형이 다르면 다른 예문이다 — 이야기 예문을 끝냈어도 일상 예문은 새로 보상받는다. */
    @Test
    void 다른_유형의_예문은_따로_보상한다() {
        UUID wordId = newWord("소쿠리");
        practiceService.practice(PARENT_ID, CHILD_ID, wordId, ExampleSentenceType.STORY, STORY_SENTENCE);
        int after = balance();

        SentencePracticeResponse daily = practiceService.practice(
                PARENT_ID, CHILD_ID, wordId, ExampleSentenceType.DAILY, DAILY_SENTENCE);

        assertThat(daily.rewarded()).isTrue();
        assertThat(balance()).isEqualTo(after + 2);
    }

    @Test
    void 하루에_두_건까지만_보상한다() {
        practiceService.practice(PARENT_ID, CHILD_ID, newWord("시아버지"),
                ExampleSentenceType.STORY, STORY_SENTENCE);
        practiceService.practice(PARENT_ID, CHILD_ID, newWord("이장"),
                ExampleSentenceType.STORY, STORY_SENTENCE);
        int after = balance();

        SentencePracticeResponse third = practiceService.practice(
                PARENT_ID, CHILD_ID, newWord("친정"), ExampleSentenceType.STORY, STORY_SENTENCE);

        assertThat(third.matched()).isTrue();
        assertThat(third.rewarded()).isFalse();
        assertThat(third.skipReason()).isEqualTo(SkipReason.DAILY_LIMIT);
        assertThat(balance()).isEqualTo(after);
    }

    /** V14 이전에 저장된 단어는 심화 예문이 없다 — 채점이 성립하지 않으므로 409로 막는다. */
    @Test
    void 없는_예문_유형이면_에러가_난다() {
        UUID wordId = newWord("갓");

        assertThatThrownBy(() -> practiceService.practice(
                PARENT_ID, CHILD_ID, wordId, ExampleSentenceType.ADVANCED, "아무 말"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXAMPLE_SENTENCE_MISSING);
    }

    @Test
    void 남의_아이_단어로는_연습할_수_없다() {
        UUID wordId = newWord("기왓장");

        assertThatThrownBy(() -> practiceService.practice(
                PARENT_ID, SIBLING_ID, wordId, ExampleSentenceType.STORY, STORY_SENTENCE))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }
}
