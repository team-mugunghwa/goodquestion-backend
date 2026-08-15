package com.mugunghwa.goodquestion.learning.wordbook;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustService;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWalletRepository;
import com.mugunghwa.goodquestion.learning.wordbook.dto.WordPracticeResponse;
import com.mugunghwa.goodquestion.user.child.ChildService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 단어 말하기 연습 (학습 → 보상 → 행성 꾸미기 사이클).
 *
 * <p>말하기 학습 서비스이므로 연습도 말하기다 — 단어장을 열어 단어를 고르고, 그 단어를 넣어
 * 문장을 만들어 말한다. 음성 인식은 클라이언트가 기존 {@code /api/stt}로 하고, 여기는
 * 인식된 <b>텍스트</b>만 받는다 — 아이 음성은 서버에 저장하지 않는다.
 *
 * <p>지급 규칙(2026-08-16 확정): 단어당 최초 1회 · 1개 · 하루 최대 3개.
 * 하루 3개는 이야기 완주 1회(3개)와 같은 무게라, 단어 연습이 이야기를 대체하지 않으면서
 * "매일 조금씩" 돌아오게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WordPracticeService {

    /** 하루의 기준. 서버가 어느 리전에 있든 아이의 하루는 한국 시간으로 센다. */
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private static final int DAILY_REWARD_LIMIT = 3;

    private final WordbookRepository wordbookRepository;
    private final WordPracticeRepository practiceRepository;
    private final StardustService stardustService;
    private final StardustWalletRepository walletRepository;
    private final ChildService childService;

    /**
     * 연습 제출. 문장에 단어가 들어 있으면 성공이고, 지급 규칙을 통과하면 별가루 1개를 준다.
     *
     * <p>판정은 관대하게 — 조사가 붙거나("가마솥에서") 띄어쓰기가 섞여도("가마 솥") 인정한다.
     * 세 번 말해서 겨우 인식된 문장을 표기 차이로 떨어뜨리면 연습이 시험이 된다.
     *
     * <p>같은 단어 동시 제출은 DB unique(wordbook_id)가 한쪽을 되돌린다. 아이 하나가
     * 버튼을 연타해야 나는 경합이라 따로 멱등 키를 받지 않는다.
     */
    @Transactional
    public WordPracticeResponse practice(UUID parentId, UUID childId, UUID wordId, String spokenText) {
        childService.getOwnedChild(parentId, childId);
        Wordbook word = wordbookRepository.findByIdAndChildId(wordId, childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "단어를 찾을 수 없습니다."));

        if (!containsWord(spokenText, word.getWord())) {
            return respond(childId, false, false, WordPracticeResponse.SkipReason.WORD_NOT_IN_SENTENCE);
        }
        if (practiceRepository.existsByWordId(word.getId())) {
            // 다시 연습하는 것은 막지 않는다 - 보상만 최초 1회다
            return respond(childId, true, false, WordPracticeResponse.SkipReason.ALREADY_REWARDED);
        }
        if (practiceRepository.countByChildIdAndCreatedAtGreaterThanEqual(childId, startOfTodayKorea())
                >= DAILY_REWARD_LIMIT) {
            // 상한에 걸린 성공은 기록하지 않는다 - 이 단어는 내일 다시 성공하면 보상받는다
            return respond(childId, true, false, WordPracticeResponse.SkipReason.DAILY_LIMIT);
        }

        practiceRepository.save(WordPractice.builder()
                .word(word).child(word.getChild()).spokenText(spokenText.trim()).build());
        stardustService.awardWordPracticed(word.getChild());
        return respond(childId, true, true, null);
    }

    /**
     * 문장 어딘가에 단어가 들어 있으면 인정한다. 공백을 모두 걷어내고 비교하므로
     * "가마 솥에서"도 "가마솥"을 담은 것으로 본다.
     */
    private static boolean containsWord(String spoken, String word) {
        if (spoken == null || spoken.isBlank()) {
            return false;
        }
        return normalize(spoken).contains(normalize(word));
    }

    private static String normalize(String text) {
        return text.replaceAll("\\s+", "");
    }

    /** 자정(Asia/Seoul)을 서버 시각으로 환산한다. created_at은 timestamptz라 그대로 비교된다. */
    private static OffsetDateTime startOfTodayKorea() {
        return LocalDate.now(KOREA).atStartOfDay(KOREA).toOffsetDateTime();
    }

    private WordPracticeResponse respond(UUID childId, boolean matched, boolean rewarded,
                                         WordPracticeResponse.SkipReason reason) {
        int balance = walletRepository.findByChildId(childId)
                .map(wallet -> wallet.getBalance()).orElse(0);
        return new WordPracticeResponse(matched, rewarded, reason, balance);
    }
}
