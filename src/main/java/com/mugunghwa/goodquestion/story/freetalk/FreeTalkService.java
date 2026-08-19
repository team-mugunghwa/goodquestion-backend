package com.mugunghwa.goodquestion.story.freetalk;

import com.mugunghwa.goodquestion.ai.freetalk.FreeTalkLlmClient;
import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.global.vocab.CharacterEmotion;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkCharacterResponse;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkEndResponse;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkLineResponse;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkMessageRequest;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkStartRequest;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkStartResponse;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkTurnResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 후속 자유 대화. 이야기를 완주한 아이가 그 이야기의 인물과 목표 없이 이야기한다.
 *
 * <p><b>학습 경로(TurnOrchestrator)와 완전히 갈라져 있다.</b> 요소 분석도, 진행 판단도,
 * 별가루도, 리포트도 없다. 여기서 일어나는 일은 세션·리포트·보상 어느 쪽에도 이벤트를
 * 보내지 않는다 - 놀이가 학습 지표를 흔들지 않게 하는 것이 이 기능의 전제다.
 *
 * <p><b>이 클래스에는 트랜잭션이 없다.</b> LLM과 TTS 왕복이 사이에 끼어 있어 한
 * 트랜잭션으로 묶으면 응답을 기다리는 내내 DB 커넥션을 쥐게 된다. DB를 만지는 구간만
 * {@link FreeTalkTransactions}가 짧게 처리한다.
 *
 * <p>세 단계가 늘 같은 순서다 - <b>준비(읽기) → 대사 생성 → 저장(쓰기)</b>. 대사가
 * 만들어지기 전에는 아무것도 남기지 않는다. LLM이 실패하면 남은 것이 없어야 정직한
 * 재시도가 처음과 같은 자리에서 다시 시작한다(멱등키가 실패한 키를 비우는 것과 짝이다).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FreeTalkService {

    private final FreeTalkTransactions transactions;
    private final FreeTalkLlmClient llmClient;
    private final FreeTalkVoice voice;

    /** 완주한 이야기의 대화 가능 인물 목록. 미완주면 404다. */
    public List<FreeTalkCharacterResponse> getCharacters(UUID parentId, UUID childId, UUID storyId) {
        return transactions.characters(parentId, childId, storyId);
    }

    /** 캐릭터의 첫 인사를 만들고, 그것이 나온 뒤에 대화를 연다. */
    public FreeTalkStartResponse start(UUID parentId, UUID childId, FreeTalkStartRequest request) {
        FreeTalkContext context = transactions.prepareStart(parentId, childId, request);
        FreeTalkLine greeting = speak(context.llmInput());
        UUID freeTalkId = transactions.openWithGreeting(parentId, childId, request, greeting);
        return new FreeTalkStartResponse(freeTalkId, context.character(),
                withVoice(greeting, context.characterName()), FreeTalkTransactions.MAX_TURNS);
    }

    /**
     * 아이의 말 한 줄을 받아 캐릭터가 답한다.
     *
     * <p>턴 상한에 닿은 턴은 마무리 대사가 되고 대화가 닫힌다. 아이에게는 "열 번을 다
     * 썼다"가 아니라 캐릭터의 작별 인사로 보인다.
     */
    public FreeTalkTurnResponse speak(UUID parentId, UUID freeTalkId,
                                      FreeTalkMessageRequest request) {
        try {
            FreeTalkContext context = transactions.prepareTurn(parentId, freeTalkId, request.text());
            FreeTalkLine reply = speak(context.llmInput());
            transactions.commitTurn(context, reply);
            return new FreeTalkTurnResponse(withVoice(reply, context.characterName()),
                    context.turnCount(), context.lastTurn());
        } catch (DataIntegrityViolationException e) {
            throw concurrentTurn(freeTalkId, e);
        }
    }

    /**
     * 아이가 먼저 그만둔다. 이미 닫힌 대화면 남아 있는 마지막 대사를 그대로 돌려준다 -
     * 끝난 대화에 "그만하기"가 한 번 더 들어오는 것은 흔한 일이고, 그때마다 LLM을
     * 다시 부르면 요금만 두 배가 된다.
     */
    public FreeTalkEndResponse end(UUID parentId, UUID freeTalkId) {
        FreeTalkEnding ending = transactions.prepareEnd(parentId, freeTalkId);
        FreeTalkLine closing = ending.alreadyEnded()
                ? ending.stored()
                : transactions.commitEnd(freeTalkId, speak(ending.llmInput()));
        return new FreeTalkEndResponse(withVoice(closing, ending.characterName()));
    }

    /**
     * 아이가 인사 없이 그냥 나간다. 대화를 닫기만 하고 <b>LLM도 TTS도 부르지 않는다.</b>
     *
     * <p>{@link #end}를 재사용하지 않은 것이 이 메서드의 전부다. 저쪽은 작별 대사를 만들어
     * 읽어 주므로 나가려는 아이가 낭독이 끝날 때까지 붙잡힌다 - "그냥 나가기"가 마무리보다
     * 오히려 느려지고 호출 요금까지 든다. 플래그 하나로 갈래를 나누지 않은 것도 같은
     * 이유다. 플래그는 어느 호출부에서든 빠뜨릴 수 있지만, 대사 생성이 아예 없는 경로에는
     * 낭독이 섞여 들어올 자리가 없다.
     *
     * <p>이미 닫힌 대화여도 성공으로 끝낸다. 아이는 응답을 기다리지 않고 나가므로 재전송이
     * 흔한데, 나가기가 거절로 막히면 그 실패는 아무 데도 보여 줄 수 없다.
     */
    public void leave(UUID parentId, UUID freeTalkId) {
        transactions.leave(parentId, freeTalkId);
    }

    /**
     * <b>트랜잭션 밖에서 부른다.</b> 모르는 감정 값은 NEUTRAL로 맞춘다.
     *
     * <p>대사가 비어 돌아오면 여기서 끊는다. 그대로 저장하러 가면 text NOT NULL에 걸려
     * 무결성 예외가 되고, 그것을 동시 제출로 읽어 409로 내보내게 된다 - 벤더 응답 형식
     * 오류가 "연타했으니 다시 보내라"로 둔갑하면 클라이언트가 영원히 재시도한다.
     */
    private FreeTalkLine speak(FreeTalkLlmClient.FreeTalkLlmInput input) {
        FreeTalkLlmClient.FreeTalkLlmResult result = llmClient.speak(input);
        if (result.text() == null || result.text().isBlank()) {
            throw new BusinessException(ErrorCode.AI_UPSTREAM_ERROR, "캐릭터 대사를 받지 못했습니다.");
        }
        return new FreeTalkLine(result.text(), toEmotion(result.emotion()));
    }

    /**
     * 합성은 저장이 끝난 뒤에 붙인다. 오디오 URL은 만료가 있어 어차피 보관하지 않으므로,
     * 합성이 실패해도 대사는 이미 남아 있어야 다음 턴의 문맥이 끊기지 않는다.
     */
    private FreeTalkLineResponse withVoice(FreeTalkLine line, String characterName) {
        return new FreeTalkLineResponse(line.text(),
                voice.synthesize(line.text(), characterName), line.emotion());
    }

    /**
     * 모르는 감정 값은 NEUTRAL로 둔다 - 표정 하나 때문에 대화를 실패시킬 이유가 없다
     * (학습 대화의 CharacterResponseService와 같은 판단).
     */
    private CharacterEmotion toEmotion(String emotion) {
        if (emotion == null) {
            return CharacterEmotion.NEUTRAL;
        }
        try {
            return CharacterEmotion.valueOf(emotion.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return CharacterEmotion.NEUTRAL;
        }
    }

    /**
     * free_talk_messages의 {@code unique (free_talk_id, turn_order)}가 걸린 것은 같은
     * 대화에 두 턴이 겹쳤다는 뜻이다. 턴 수 조건부 갱신이 먼저 걸러 내므로 여기까지
     * 오는 일은 드물지만, 순번을 마지막 값 더하기 1로 잡는 이상 마지막 방어선은 제약이다.
     *
     * <p>원래 예외는 로그로 남긴다. 무결성 충돌이 동시 제출이 아닌 다른 이유로 났다면
     * 그것은 결함이고, 409로 덮어쓰기만 하면 드러날 자리가 없어진다.
     */
    private BusinessException concurrentTurn(UUID freeTalkId, DataIntegrityViolationException e) {
        log.warn("자유 대화 중 무결성 충돌 freeTalkId={}", freeTalkId, e);
        return new BusinessException(ErrorCode.CONCURRENT_TURN);
    }
}
