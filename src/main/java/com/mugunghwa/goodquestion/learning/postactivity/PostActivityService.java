package com.mugunghwa.goodquestion.learning.postactivity;

import com.mugunghwa.goodquestion.learning.postactivity.dto.*;
import com.mugunghwa.goodquestion.story.session.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostActivityService {

    private final PostActivityResultRepository resultRepository;
    private final SessionService sessionService;
    // TODO: ReportService 주입 (완료 시 리포트 생성 트리거)

    public PostActivityStartResponse start(UUID parentId, UUID sessionId) {
        // TODO: 세션 소유·POST_ACTIVITY 상태 검증 → stories.post_activity_config의
        //  cards를 "무작위 순서"로 반환, 결과 행 없으면 생성
        throw new UnsupportedOperationException("TODO");
    }

    @Transactional
    public CardSubmitResponse submitOrder(UUID parentId, UUID sessionId, CardSubmitRequest request) {
        // TODO: 정답 여부는 서버가 config.cards[].correct_order와 비교해 계산 (프런트 판정 금지)
        //  정답 → retelling_keywords 반환 / 오답 → attempt_count 증가 후 재시도
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * 재구성 발화 제출 = 세션 완료 + 별가루 지급(활동-09~10, 보상-04).
     * 지급 결과를 응답에 담아야 하므로 완료 이벤트는 같은 트랜잭션에서 동기로 처리한다.
     */
    @Transactional
    public RetellingResponse submitRetelling(UUID parentId, UUID sessionId, RetellingRequest request) {
        // TODO: 카드 정답 전이면 409 RETELLING_BEFORE_ORDER
        //  → retelling_text 저장 → completed_at 기록 → session.complete()
        //  → 별가루 지급(멱등) → 이번 완주로 해금된 아이템 계산 → 리포트 생성 트리거
        throw new UnsupportedOperationException("미구현: 재구성 발화 제출");
    }
}
