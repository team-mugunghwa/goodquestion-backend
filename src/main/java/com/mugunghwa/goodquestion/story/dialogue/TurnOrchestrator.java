package com.mugunghwa.goodquestion.story.dialogue;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.story.dialogue.CharacterResponseService.CharacterReply;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceRequest;
import com.mugunghwa.goodquestion.story.dialogue.dto.UtteranceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 발화 처리 파이프라인 오케스트레이터.
 * 분석·진행·표현의 책임 분리 원칙: 각 단계는 하위 컴포넌트가 담당하고 여기서는 순서만 조율한다.
 * 미션 수행 결과(request.missionId 존재)도 동일 파이프라인으로 분석·누적한다 —
 * 미션 check_points가 장면 target_elements와 대응하므로 별도 채점 없이 요소 충족으로 확인한다.
 *
 * <p><b>이 메서드에는 트랜잭션이 없다.</b> 외부 LLM 호출 두 번이 사이에 끼어 있어 한 트랜잭션으로
 * 묶으면 응답을 기다리는 내내 DB 커넥션을 쥐게 된다. DB를 만지는 구간만
 * {@link TurnTransactions}가 짧은 트랜잭션으로 처리하고, 구간 사이는 엔티티가 아니라 값으로
 * 건넌다(docs/트러블슈팅_턴_처리_커넥션_점유.md).
 *
 * <p>순서 자체가 규칙이다. 아래 다섯 단계는 앞 단계의 결과가 뒤 단계의 입력이라 바꿔 끼울 수
 * 없다. 특히 분석을 저장보다 먼저 두는 것은 의도한 것이다 - 첫 외부 호출 전까지 커밋된 것이
 * 없어야 "발화만 남고 분석은 없는" 상태가 생기지 않는다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TurnOrchestrator {

    private final TurnTransactions turnTransactions;
    private final UtteranceAnalysisService analysisService;
    private final CharacterResponseService characterResponseService;

    /**
     * 구간별 소요 시간을 한 줄로 남긴다. 실패해도 남겨야 한다 - 외부 호출이 타임아웃으로
     * 끊겼을 때가 이 숫자를 가장 보고 싶은 순간이다.
     */
    public UtteranceResponse processUtterance(UUID parentId, UUID sessionId, UtteranceRequest request) {
        TurnTimer timer = TurnTimer.start();
        try {
            // ① 소유권·상태 확인과 분석 입력 준비 (트랜잭션)
            TurnPreparation preparation = turnTransactions.prepare(parentId, sessionId);
            timer.mark("준비");

            // ② 발화 분석. 직전 캐릭터 대사를 함께 넘겨야 무엇에 대한 대답인지 판단할 수 있다.
            AnalysisOutcome outcome = analysisService.analyze(
                    request.text(), preparation.analysisContext(), preparation.previousCharacterText());
            timer.mark("분석");

            // ③ 발화·분석 저장, 누적 갱신, 진행 판단, 미션 노출 (트랜잭션)
            ChildTurnRecord record = turnTransactions.recordChildTurn(
                    parentId, sessionId, request, outcome);
            timer.mark("판단");

            // ④ 캐릭터 대사 생성. 고정 마무리 대사가 있는 CLOSING 턴은 LLM을 부르지 않는다.
            CharacterReply reply = characterResponseService.reply(record.characterPrompt());
            timer.mark("대사");

            // ⑤ 대사 저장과 장면 이동 (트랜잭션)
            TurnClosure closure = turnTransactions.recordCharacterReply(
                    parentId, sessionId, record.decision(), reply);
            timer.mark("반영");

            // safety는 위험 신호 감지가 붙기 전까지 항상 null이다 (SafetyResponse의 TODO).
            return new UtteranceResponse(
                    record.childMessage(), record.analysis(), record.progress(),
                    closure.message(), record.mission(), closure.transition(), null);
        } catch (DataIntegrityViolationException e) {
            throw concurrentTurn(sessionId, e);
        } finally {
            log.info("턴 처리 sessionId={} {}", sessionId, timer.summary());
        }
    }

    /**
     * 턴 처리 중의 무결성 충돌은 같은 세션에 두 턴이 겹쳤다는 뜻이다.
     *
     * <p>messages의 {@code unique (session_id, turn_order)}가 걸린다. turn_order를 마지막 값
     * 더하기 1로 계산하므로, 앞선 턴이 커밋되기 전에 다음 턴이 값을 읽으면 둘이 같은 번호를
     * 잡는다. 트랜잭션을 쪼개면서 이 경로가 늘었다 - 전에는 턴 전체가 한 트랜잭션이라 낙관적
     * 락이 먼저 잡았지만, 지금은 발화 저장이 먼저 커밋되기 때문이다.
     *
     * <p>변환을 전역 핸들러가 아니라 여기서 하는 이유는 의미가 여기서만 성립하기 때문이다.
     * 가입 이메일 중복이나 배치 칸 중복도 같은 예외로 올라오는데, 그것까지 "앞선 발화를 처리하는
     * 중"이라고 답하면 클라이언트를 잘못 안내하게 된다.
     *
     * <p>원래 예외는 로그로 남긴다. 무결성 충돌이 동시 발화가 아닌 다른 이유로 났다면 그것은
     * 결함이고, 409로 덮어쓰기만 하면 드러날 자리가 없어진다.
     */
    private BusinessException concurrentTurn(UUID sessionId, DataIntegrityViolationException e) {
        log.warn("턴 처리 중 무결성 충돌 sessionId={}", sessionId, e);
        return new BusinessException(ErrorCode.CONCURRENT_TURN);
    }
}
