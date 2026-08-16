package com.mugunghwa.goodquestion.learning.wordbook;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustService;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustTransaction;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWalletRepository;
import com.mugunghwa.goodquestion.learning.wordbook.dto.SentencePracticeResponse;
import com.mugunghwa.goodquestion.user.child.ChildService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 예문 따라 말하기 (학습 → 보상 → 행성 꾸미기 사이클).
 *
 * <p>단어 말하기 연습(WordPracticeService)이 단어로 새 문장을 만드는 학습이라면, 이쪽은
 * 예문 3종(이야기/일상/심화) 중 하나를 골라 그대로 따라 말하는 학습이다. 음성 인식은
 * 클라이언트가 기존 {@code /api/stt}로 하고, 여기는 인식된 <b>텍스트</b>만 받는다 —
 * 아이 음성은 서버에 저장하지 않는다.
 *
 * <p>지급 규칙(2026-08-16 확정): 일치율 90% 이상 · 예문(단어 x 유형)당 최초 1회 · 2개 ·
 * 하루 최대 2건. 하루 4개는 완주 최대치(3+2)를 넘지 않아, 따라 말하기가 이야기를
 * 대체하지 않으면서 "매일 조금씩" 돌아오게 한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SentencePracticeService {

    /** 하루의 기준. 서버가 어느 리전에 있든 아이의 하루는 한국 시간으로 센다. */
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    /**
     * 지급 기준 일치율. 띄어쓰기·문장부호를 걷어낸 문자 일치율이라 짧은 예문에서 한 글자쯤
     * 달라도 대체로 90%를 넘는다 — 아이 발음을 시험하는 게 아니라 따라 말하기를 끝까지
     * 해냈는지 본다.
     */
    private static final BigDecimal REWARD_THRESHOLD = new BigDecimal("0.90");

    private static final int DAILY_REWARD_LIMIT = 2;

    private final WordbookRepository wordbookRepository;
    private final SentencePracticeRepository practiceRepository;
    private final StardustService stardustService;
    private final StardustWalletRepository walletRepository;
    private final ChildService childService;

    /**
     * 따라 말하기 제출. 일치율이 기준을 넘으면 성공이고, 지급 규칙을 통과하면 별가루 2개를 준다.
     *
     * <p>같은 예문 동시 제출은 DB unique(wordbook_id, sentence_type)가 한쪽을 되돌린다.
     * 아이 하나가 버튼을 연타해야 나는 경합이라 따로 멱등 키를 받지 않는다.
     */
    @Transactional
    public SentencePracticeResponse practice(UUID parentId, UUID childId, UUID wordId,
                                             ExampleSentenceType sentenceType, String spokenText) {
        childService.getOwnedChild(parentId, childId);
        Wordbook word = wordbookRepository.findByIdAndChildId(wordId, childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "단어를 찾을 수 없습니다."));

        String target = sentenceType.from(word);
        if (target == null || target.isBlank()) {
            // V14 이전에 저장된 단어는 일상/심화 예문이 없다. 화면은 있는 예문만 보여 주지만,
            // 서버도 같은 이유로 막는다 - 빈 목표로는 채점 자체가 성립하지 않는다.
            throw new BusinessException(ErrorCode.EXAMPLE_SENTENCE_MISSING);
        }

        BigDecimal similarity = SentenceSimilarity.score(target, spokenText);
        if (similarity.compareTo(REWARD_THRESHOLD) < 0) {
            return respond(childId, false, similarity, target, null, null);
        }
        if (practiceRepository.existsByWordIdAndSentenceType(word.getId(), sentenceType)) {
            // 다시 연습하는 것은 막지 않는다 - 보상만 예문당 최초 1회다
            return respond(childId, true, similarity, target, null,
                    SentencePracticeResponse.SkipReason.ALREADY_REWARDED);
        }
        if (practiceRepository.countByChildIdAndCreatedAtGreaterThanEqual(childId, startOfTodayKorea())
                >= DAILY_REWARD_LIMIT) {
            // 상한에 걸린 성공은 기록하지 않는다 - 이 예문은 내일 다시 성공하면 보상받는다
            return respond(childId, true, similarity, target, null,
                    SentencePracticeResponse.SkipReason.DAILY_LIMIT);
        }

        practiceRepository.save(SentencePractice.builder()
                .word(word).child(word.getChild()).sentenceType(sentenceType)
                .spokenText(spokenText.trim()).similarity(similarity).build());
        StardustTransaction awarded = stardustService.awardSentencePracticed(word.getChild());
        return respond(childId, true, similarity, target, awarded, null);
    }

    /** 자정(Asia/Seoul)을 서버 시각으로 환산한다. created_at은 timestamptz라 그대로 비교된다. */
    private static OffsetDateTime startOfTodayKorea() {
        return LocalDate.now(KOREA).atStartOfDay(KOREA).toOffsetDateTime();
    }

    private SentencePracticeResponse respond(UUID childId, boolean matched, BigDecimal similarity,
                                             String target, StardustTransaction awarded,
                                             SentencePracticeResponse.SkipReason reason) {
        int balance = walletRepository.findByChildId(childId)
                .map(wallet -> wallet.getBalance()).orElse(0);
        return new SentencePracticeResponse(matched, similarity, target,
                awarded != null, reason, awarded != null ? awarded.getAmount() : 0, balance);
    }
}
