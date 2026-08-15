package com.mugunghwa.goodquestion.learning.wordbook;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWalletRepository;
import com.mugunghwa.goodquestion.learning.wordbook.dto.WordCreateRequest;
import com.mugunghwa.goodquestion.learning.wordbook.dto.WordPracticeResponse;
import com.mugunghwa.goodquestion.learning.wordbook.dto.WordPracticeResponse.SkipReason;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 단어 말하기 연습 — 단어당 최초 1회 · 1개 · 하루 최대 3개.
 *
 * <p>지우(잔액 14)로 검증한다. 단어는 뜻을 함께 넣어 만들어 LLM 호출 없이 저장된다.
 */
@IntegrationTest
@Transactional
class WordPracticeServiceTest {

    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");
    private static final UUID SIBLING_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000002");

    @Autowired
    private WordPracticeService practiceService;

    @Autowired
    private WordbookService wordbookService;

    @Autowired
    private StardustWalletRepository walletRepository;

    private UUID newWord(String word) {
        return wordbookService.create(PARENT_ID, CHILD_ID,
                new WordCreateRequest(word, WordEntryType.UNKNOWN, null,
                        "테스트용 뜻", null)).id();
    }

    private int balance() {
        return walletRepository.findByChildId(CHILD_ID).orElseThrow().getBalance();
    }

    @Test
    void 단어를_넣어_말하면_별가루가_들어온다() {
        UUID wordId = newWord("가마솥");
        int before = balance();

        WordPracticeResponse response = practiceService.practice(
                PARENT_ID, CHILD_ID, wordId, "옛날에는 가마솥에서 밥을 지었어요");

        assertThat(response.matched()).isTrue();
        assertThat(response.rewarded()).isTrue();
        assertThat(response.stardustBalance()).isEqualTo(before + 1);
    }

    /** 조사가 붙거나 띄어쓰기가 섞여도 인정한다 — 표기 차이로 떨어뜨리면 연습이 시험이 된다. */
    @Test
    void 띄어쓰기가_섞여도_단어로_인정한다() {
        UUID wordId = newWord("기왓장");

        WordPracticeResponse response = practiceService.practice(
                PARENT_ID, CHILD_ID, wordId, "방귀 소리에 기왓 장이 들썩였어요");

        assertThat(response.matched()).isTrue();
        assertThat(response.rewarded()).isTrue();
    }

    @Test
    void 문장에_단어가_없으면_보상도_기록도_없다() {
        UUID wordId = newWord("배나무");
        int before = balance();

        WordPracticeResponse response = practiceService.practice(
                PARENT_ID, CHILD_ID, wordId, "오늘 날씨가 좋아요");

        assertThat(response.matched()).isFalse();
        assertThat(response.rewarded()).isFalse();
        assertThat(response.skipReason()).isEqualTo(SkipReason.WORD_NOT_IN_SENTENCE);
        assertThat(balance()).isEqualTo(before);

        // 기록이 없으므로 같은 단어로 다시 성공하면 그때 보상받는다
        assertThat(practiceService.practice(PARENT_ID, CHILD_ID, wordId,
                "배나무에 배가 주렁주렁 열렸어요").rewarded()).isTrue();
    }

    @Test
    void 같은_단어는_한_번만_보상한다() {
        UUID wordId = newWord("부지깽이");
        practiceService.practice(PARENT_ID, CHILD_ID, wordId, "부지깽이로 아궁이를 뒤적였어요");
        int after = balance();

        WordPracticeResponse second = practiceService.practice(
                PARENT_ID, CHILD_ID, wordId, "부지깽이를 들고 왔어요");

        assertThat(second.matched()).isTrue();       // 연습 자체는 언제든 환영
        assertThat(second.rewarded()).isFalse();
        assertThat(second.skipReason()).isEqualTo(SkipReason.ALREADY_REWARDED);
        assertThat(balance()).isEqualTo(after);
    }

    @Test
    void 하루에_세_개까지만_보상한다() {
        practiceService.practice(PARENT_ID, CHILD_ID, newWord("소쿠리"), "소쿠리에 나물을 담았어요");
        practiceService.practice(PARENT_ID, CHILD_ID, newWord("시아버지"), "시아버지가 깜짝 놀랐어요");
        practiceService.practice(PARENT_ID, CHILD_ID, newWord("이장"), "이장 어른을 만났어요");
        int after = balance();

        WordPracticeResponse fourth = practiceService.practice(
                PARENT_ID, CHILD_ID, newWord("친정"), "친정에 가는 길이에요");

        assertThat(fourth.matched()).isTrue();
        assertThat(fourth.rewarded()).isFalse();
        assertThat(fourth.skipReason()).isEqualTo(SkipReason.DAILY_LIMIT);
        assertThat(balance()).isEqualTo(after);
    }

    @Test
    void 남의_아이_단어로는_연습할_수_없다() {
        UUID wordId = newWord("갓");

        assertThatThrownBy(() -> practiceService.practice(PARENT_ID, SIBLING_ID, wordId, "갓을 쓴 어른"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }
}
