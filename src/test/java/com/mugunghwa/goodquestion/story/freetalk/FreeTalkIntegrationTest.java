package com.mugunghwa.goodquestion.story.freetalk;

import com.mugunghwa.goodquestion.ai.freetalk.FreeTalkPromptBuilder;
import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.global.idempotency.IdempotencyService;
import com.mugunghwa.goodquestion.global.idempotency.IdempotentEndpoint;
import com.mugunghwa.goodquestion.global.vocab.CharacterEmotion;
import com.mugunghwa.goodquestion.learning.reward.stardust.StardustWalletRepository;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkCharacterResponse;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkEndResponse;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkMessageRequest;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkStartRequest;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkStartResponse;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkTurnResponse;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 후속 자유 대화를 실제 경로로 확인한다.
 *
 * <p>보는 것은 네 가지다 - 완주한 아이만 들어올 수 있는가, 열 턴에서 캐릭터가 스스로
 * 마무리하는가, 같은 멱등키의 재전송이 LLM을 다시 부르지 않는가, 그리고 <b>학습 쪽에
 * 아무 일도 일어나지 않는가</b>. 마지막 하나가 이 기능의 전제다 - 놀이가 리포트와
 * 별가루를 흔들면 이야기 완주의 가치가 무너진다.
 *
 * <p>LLM과 TTS만 대역으로 바꾸고 나머지는 실제 경로를 탄다.
 */
@IntegrationTest
@Transactional
@Import(StubFreeTalkConfig.class)
class FreeTalkIntegrationTest {

    /** R__2_seed_demo_data.sql의 데모 계정. 보호자 "김보호" / 아이 "지우". */
    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");
    /** 지우가 완주한 이야기 - 시드에 COMPLETED 세션이 있다. */
    private static final UUID COMPLETED_STORY_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    /** R__3의 카드만 있는 이야기 - 지우가 한 번도 하지 않았다. */
    private static final UUID UNPLAYED_STORY_ID =
            UUID.fromString("11111111-1111-1111-1111-000000000021");

    @Autowired
    private FreeTalkService freeTalkService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private StubFreeTalkLlmClient llmClient;

    @Autowired
    private StubFreeTalkVoice voice;

    @Autowired
    private StardustWalletRepository walletRepository;

    @Autowired
    private FreeTalkTransactions freeTalkTransactions;

    @Autowired
    private FreeTalkRepository freeTalkRepository;

    @Autowired
    private FreeTalkMessageRepository freeTalkMessageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void 대역을_비운다() {
        llmClient.reset();
        voice.reset();
    }

    private UUID 대화를_연다() {
        return freeTalkService.start(PARENT_ID, CHILD_ID,
                new FreeTalkStartRequest(COMPLETED_STORY_ID, 며느리().characterId())).freeTalkId();
    }

    private FreeTalkCharacterResponse 며느리() {
        return freeTalkService.getCharacters(PARENT_ID, CHILD_ID, COMPLETED_STORY_ID).getFirst();
    }

    private FreeTalkTurnResponse 말한다(UUID freeTalkId, String text) {
        return freeTalkService.speak(PARENT_ID, freeTalkId, new FreeTalkMessageRequest(text));
    }

    @Test
    void 완주한_이야기의_인물을_모두_고를_수_있다() {
        List<FreeTalkCharacterResponse> characters =
                freeTalkService.getCharacters(PARENT_ID, CHILD_ID, COMPLETED_STORY_ID);

        assertThat(characters).hasSize(3);
        assertThat(characters).extracting(FreeTalkCharacterResponse::name)
                .containsExactlyInAnyOrder("방귀쟁이 며느리", "시아버지", "마을 이장");
        // 아직 이야기한 적이 없으면 마지막 대화 시각은 비어 있다.
        assertThat(characters).allSatisfy(character ->
                assertThat(character.lastTalkedAt()).isNull());
    }

    @Test
    void 완주하지_않은_이야기는_인물_목록을_주지_않는다() {
        assertThatThrownBy(() ->
                freeTalkService.getCharacters(PARENT_ID, CHILD_ID, UNPLAYED_STORY_ID))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STORY_NOT_COMPLETED));
    }

    @Test
    void 완주하지_않은_이야기로는_대화를_시작할_수_없다() {
        UUID characterId = 며느리().characterId();

        assertThatThrownBy(() -> freeTalkService.start(PARENT_ID, CHILD_ID,
                new FreeTalkStartRequest(UNPLAYED_STORY_ID, characterId)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.STORY_NOT_COMPLETED));
    }

    @Test
    void 이_이야기의_인물이_아니면_붙일_수_없다() {
        // 다른 세계의 인물을 붙이면 그 인물이 모르는 이야기를 아는 척하게 된다.
        assertThatThrownBy(() -> freeTalkService.start(PARENT_ID, CHILD_ID,
                new FreeTalkStartRequest(COMPLETED_STORY_ID, UUID.randomUUID())))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void 대화를_시작하면_캐릭터가_먼저_인사한다() {
        FreeTalkCharacterResponse character = 며느리();

        FreeTalkStartResponse response = freeTalkService.start(PARENT_ID, CHILD_ID,
                new FreeTalkStartRequest(COMPLETED_STORY_ID, character.characterId()));

        assertThat(response.freeTalkId()).isNotNull();
        assertThat(response.character().characterId()).isEqualTo(character.characterId());
        assertThat(response.opening().text()).isNotBlank();
        assertThat(response.opening().audioUrl()).isEqualTo(StubFreeTalkConfig.STUB_AUDIO_URL);
        assertThat(response.maxTurns()).isEqualTo(10);
        // 첫 대사는 유도가 아니라 인사다 - 프롬프트 단계로 확인한다.
        assertThat(llmClient.lastStage).isEqualTo(FreeTalkPromptBuilder.STAGE_OPENING);
    }

    @Test
    void 열_번째_턴에서_캐릭터가_마무리하고_대화가_닫힌다() {
        UUID freeTalkId = 대화를_연다();

        for (int turn = 1; turn <= 9; turn++) {
            FreeTalkTurnResponse response = 말한다(freeTalkId, "그때 무서웠어?");
            assertThat(response.turnCount()).isEqualTo(turn);
            assertThat(response.ended()).isFalse();
            assertThat(llmClient.lastStage).isEqualTo(FreeTalkPromptBuilder.STAGE_TALK);
        }

        FreeTalkTurnResponse last = 말한다(freeTalkId, "이제 안 부끄러워?");

        assertThat(last.turnCount()).isEqualTo(10);
        assertThat(last.ended()).isTrue();
        // 마지막 대사는 "열 번 다 썼다"가 아니라 캐릭터의 작별이어야 한다.
        assertThat(llmClient.lastStage).isEqualTo(FreeTalkPromptBuilder.STAGE_CLOSING);
        assertThat(last.characterMessage().text()).isNotBlank();
    }

    @Test
    void 닫힌_대화에는_더_말할_수_없다() {
        UUID freeTalkId = 대화를_연다();
        for (int turn = 1; turn <= 10; turn++) {
            말한다(freeTalkId, "그래서 어떻게 됐어?");
        }

        assertThatThrownBy(() -> 말한다(freeTalkId, "한 번만 더!"))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FREE_TALK_ENDED));
    }

    @Test
    void 아이가_먼저_그만두면_작별_대사를_받고_닫힌다() {
        UUID freeTalkId = 대화를_연다();
        말한다(freeTalkId, "안녕!");

        FreeTalkEndResponse response = freeTalkService.end(PARENT_ID, freeTalkId);

        assertThat(response.closing().text()).isNotBlank();
        assertThat(llmClient.lastStage).isEqualTo(FreeTalkPromptBuilder.STAGE_CLOSING);
        assertThatThrownBy(() -> 말한다(freeTalkId, "그래도 더 이야기하고 싶어"))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FREE_TALK_ENDED));
    }

    @Test
    void 이미_닫힌_대화의_그만하기는_LLM을_다시_부르지_않는다() {
        UUID freeTalkId = 대화를_연다();
        말한다(freeTalkId, "안녕!");
        FreeTalkEndResponse first = freeTalkService.end(PARENT_ID, freeTalkId);
        int callsAfterFirst = llmClient.calls;

        FreeTalkEndResponse again = freeTalkService.end(PARENT_ID, freeTalkId);

        assertThat(again.closing().text()).isEqualTo(first.closing().text());
        assertThat(llmClient.calls).isEqualTo(callsAfterFirst);
    }

    @Test
    void 같은_멱등키의_재전송은_턴을_늘리지_않고_저장된_응답을_재생한다() {
        UUID freeTalkId = 대화를_연다();

        FreeTalkTurnResponse first = submit(freeTalkId, "key-1", "그때 기분이 어땠어?");
        int callsAfterFirst = llmClient.calls;

        FreeTalkTurnResponse replayed = submit(freeTalkId, "key-1", "그때 기분이 어땠어?");

        assertThat(replayed.turnCount()).isEqualTo(first.turnCount());
        assertThat(replayed.characterMessage().text()).isEqualTo(first.characterMessage().text());
        // 재전송이 대사를 다시 만들지 않았다 - 중복 과금이 나지 않는다는 뜻이다.
        assertThat(llmClient.calls).isEqualTo(callsAfterFirst);
        assertThat(messageCount(freeTalkId)).isEqualTo(3);
    }

    @Test
    void 새_멱등키는_새_턴으로_처리된다() {
        UUID freeTalkId = 대화를_연다();

        submit(freeTalkId, "key-1", "그때 기분이 어땠어?");
        FreeTalkTurnResponse second = submit(freeTalkId, "key-2", "지금은 괜찮아?");

        assertThat(second.turnCount()).isEqualTo(2);
    }

    @Test
    void 남의_대화에는_손댈_수_없다() {
        UUID freeTalkId = 대화를_연다();

        assertThatThrownBy(() -> freeTalkService.speak(UUID.randomUUID(), freeTalkId,
                new FreeTalkMessageRequest("안녕")))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    @Test
    void 자유_대화는_별가루와_학습_대화_기록을_건드리지_않는다() {
        int balanceBefore = balance();
        long learningMessagesBefore = learningMessageCount();
        long sessionsBefore = sessionCount();

        UUID freeTalkId = 대화를_연다();
        for (int turn = 1; turn <= 10; turn++) {
            말한다(freeTalkId, "그다음엔 어떻게 했어?");
        }

        assertThat(balance()).isEqualTo(balanceBefore);
        assertThat(learningMessageCount()).isEqualTo(learningMessagesBefore);
        assertThat(sessionCount()).isEqualTo(sessionsBefore);
    }

    @Test
    void 다시_이야기하면_마지막_대화_시각이_남는다() {
        대화를_연다();

        FreeTalkCharacterResponse character = 며느리();

        assertThat(character.lastTalkedAt()).isNotNull();
    }

    @Test
    void 대사_생성이_실패하면_아무것도_남지_않는다() {
        UUID freeTalkId = 대화를_연다();
        llmClient.willFail();

        assertThatThrownBy(() -> 말한다(freeTalkId, "그때 어땠어?"))
                .isInstanceOf(IllegalStateException.class);

        // 첫 인사 하나뿐이다 - 아이 말만 남고 답이 없는 턴이 생기지 않았다.
        assertThat(messageCount(freeTalkId)).isEqualTo(1);

        // 그래서 정직한 재시도가 처음과 같은 자리에서 다시 시작한다.
        FreeTalkTurnResponse retried = 말한다(freeTalkId, "그때 어땠어?");
        assertThat(retried.turnCount()).isEqualTo(1);
        assertThat(messageCount(freeTalkId)).isEqualTo(3);
    }

    @Test
    void 마지막_턴의_대사_생성이_실패해도_턴_상한이_새지_않는다() {
        UUID freeTalkId = 대화를_연다();
        for (int turn = 1; turn <= 9; turn++) {
            말한다(freeTalkId, "그다음엔?");
        }

        llmClient.willFail();
        assertThatThrownBy(() -> 말한다(freeTalkId, "마지막으로 물어볼게"))
                .isInstanceOf(IllegalStateException.class);

        // turn_count와 ended_at이 갈리면 여기서 11번째 턴이 열린다.
        FreeTalkTurnResponse tenth = 말한다(freeTalkId, "마지막으로 물어볼게");
        assertThat(tenth.turnCount()).isEqualTo(10);
        assertThat(tenth.ended()).isTrue();
        assertThatThrownBy(() -> 말한다(freeTalkId, "한 번만 더"))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FREE_TALK_ENDED));
    }

    @Test
    void 첫_인사_생성이_실패하면_빈_대화가_남지_않는다() {
        UUID characterId = 며느리().characterId();
        long before = freeTalkRepository.count();
        llmClient.willFail();

        assertThatThrownBy(() -> freeTalkService.start(PARENT_ID, CHILD_ID,
                new FreeTalkStartRequest(COMPLETED_STORY_ID, characterId)))
                .isInstanceOf(IllegalStateException.class);

        // 아이가 다시 들어갈 방법이 없는 대화가 쌓이면 "마지막으로 이야기한 때"까지 거짓이 된다.
        assertThat(freeTalkRepository.count()).isEqualTo(before);
    }

    @Test
    void 겹친_턴은_뒤늦은_쪽이_거절된다() {
        UUID freeTalkId = 대화를_연다();

        // 준비와 저장 사이에 LLM 왕복이 있다 - 그 틈에 두 요청이 같은 턴을 읽는 상황이다.
        FreeTalkContext first = freeTalkTransactions.prepareTurn(PARENT_ID, freeTalkId, "먼저 말했어");
        FreeTalkContext second = freeTalkTransactions.prepareTurn(PARENT_ID, freeTalkId, "같이 말했어");

        freeTalkTransactions.commitTurn(first,
                new FreeTalkLine("그랬구나.", CharacterEmotion.HAPPY));

        assertThatThrownBy(() -> freeTalkTransactions.commitTurn(second,
                new FreeTalkLine("응?", CharacterEmotion.HAPPY)))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.CONCURRENT_TURN));

        // 진 쪽은 아무것도 남기지 않는다 - 첫 인사 + 이긴 턴의 두 줄이 전부다.
        assertThat(messageCount(freeTalkId)).isEqualTo(3);
    }

    @Test
    void 인사_없이_나가면_대사도_음성도_만들지_않고_대화가_닫힌다() {
        UUID freeTalkId = 대화를_연다();
        말한다(freeTalkId, "안녕!");
        int llmCallsBefore = llmClient.calls;
        int voiceCallsBefore = voice.calls;
        int messagesBefore = messageCount(freeTalkId);

        freeTalkService.leave(PARENT_ID, freeTalkId);

        // 이 기능의 존재 이유다 - 나가려는 아이를 작별 낭독이 끝날 때까지 붙잡지 않는다.
        assertThat(llmClient.calls).isEqualTo(llmCallsBefore);
        assertThat(voice.calls).isEqualTo(voiceCallsBefore);
        // 횟수만 같은 것이 아니라 마무리 프롬프트가 조립조차 되지 않았다.
        assertThat(llmClient.lastStage).isEqualTo(FreeTalkPromptBuilder.STAGE_TALK);
        // 작별 대사 줄도 남지 않았다.
        assertThat(messageCount(freeTalkId)).isEqualTo(messagesBefore);

        // 그래도 대화는 닫혔다 - 안 닫으면 ended_at이 빈 행이 영원히 열린 채 남는다.
        assertThat(freeTalkRepository.findById(freeTalkId).orElseThrow().isEnded()).isTrue();
        assertThatThrownBy(() -> 말한다(freeTalkId, "역시 더 이야기할래"))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FREE_TALK_ENDED));
    }

    @Test
    void 이미_닫힌_대화에_다시_나가도_성공한다() {
        UUID freeTalkId = 대화를_연다();
        freeTalkService.leave(PARENT_ID, freeTalkId);
        int llmCallsBefore = llmClient.calls;
        int voiceCallsBefore = voice.calls;

        // 아이는 응답을 기다리지 않고 나가므로 재전송이 흔하다. 여기서 409를 던지면 못 나간다.
        assertThatCode(() -> freeTalkService.leave(PARENT_ID, freeTalkId))
                .doesNotThrowAnyException();

        assertThat(llmClient.calls).isEqualTo(llmCallsBefore);
        assertThat(voice.calls).isEqualTo(voiceCallsBefore);
        assertThat(freeTalkRepository.findById(freeTalkId).orElseThrow().isEnded()).isTrue();
    }

    @Test
    void 남의_대화는_나가기로도_닫을_수_없다() {
        UUID freeTalkId = 대화를_연다();

        assertThatThrownBy(() -> freeTalkService.leave(UUID.randomUUID(), freeTalkId))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        // 거절만 한 것이 아니라 남의 대화를 닫아 주지도 않았다.
        assertThat(freeTalkRepository.findById(freeTalkId).orElseThrow().isEnded()).isFalse();
    }

    @Test
    void 없는_대화는_나갈_수_없다() {
        assertThatThrownBy(() -> freeTalkService.leave(PARENT_ID, UUID.randomUUID()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    @Test
    void 나간_뒤에_들어온_마무리하기는_저장된_마지막_대사를_그대로_돌려준다() {
        UUID freeTalkId = 대화를_연다();
        FreeTalkTurnResponse turn = 말한다(freeTalkId, "안녕!");
        freeTalkService.leave(PARENT_ID, freeTalkId);
        int llmCallsBefore = llmClient.calls;

        // 나가기와 마무리하기가 겹쳐 들어올 수 있다. 뒤늦은 쪽이 새 작별 대사를 만들면
        // 아이는 이미 나간 뒤인데 요금만 나간다 - 기존 end()의 "이미 닫힘" 갈래가 물려야 한다.
        FreeTalkEndResponse response = freeTalkService.end(PARENT_ID, freeTalkId);

        assertThat(response.closing().text()).isEqualTo(turn.characterMessage().text());
        assertThat(llmClient.calls).isEqualTo(llmCallsBefore);
    }

    // ----- 도우미 -----

    private FreeTalkTurnResponse submit(UUID freeTalkId, String key, String text) {
        return idempotencyService.execute(IdempotentEndpoint.FREE_TALK_MESSAGE, freeTalkId,
                PARENT_ID, key, FreeTalkTurnResponse.class,
                () -> freeTalkService.speak(PARENT_ID, freeTalkId,
                        new FreeTalkMessageRequest(text)));
    }

    private int messageCount(UUID freeTalkId) {
        return freeTalkMessageRepository.findAllByFreeTalkIdOrderByTurnOrderAsc(freeTalkId).size();
    }

    /**
     * 테스트가 한 트랜잭션이라 JPA가 아직 안 내보낸 변경은 JDBC 조회에 안 보인다.
     * "아무 일도 없었다"를 확인하는 조회라 안 보이면 통과해 버린다 - 먼저 내보낸다.
     */
    private long learningMessageCount() {
        entityManager.flush();
        return jdbcTemplate.queryForObject("select count(*) from messages", Long.class);
    }

    private long sessionCount() {
        entityManager.flush();
        return jdbcTemplate.queryForObject("select count(*) from story_sessions", Long.class);
    }

    private int balance() {
        return walletRepository.findByChildId(CHILD_ID).orElseThrow().getBalance();
    }

}
