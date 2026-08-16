package com.mugunghwa.goodquestion.learning.wordbook;

import com.mugunghwa.goodquestion.ai.word.WordMeaningLlmClient;
import com.mugunghwa.goodquestion.ai.word.WordMeaningLlmClient.WordMeaningResult;
import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.learning.wordbook.dto.WordCreateRequest;
import com.mugunghwa.goodquestion.learning.wordbook.dto.WordResponse;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@IntegrationTest
@Transactional
class WordbookServiceTest {

    /**
     * Flyway가 적용하는 R__2_seed_demo_data.sql의 데모 데이터를 전제한다.
     * 보호자 "김보호"에게 아이 "지우"와 "하준"이 있고, 단어장은 지우에게만 들어 있다.
     */
    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");
    private static final UUID OTHER_PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000002");
    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000001");
    private static final UUID SIBLING_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-000000000002");
    private static final UUID SCENE_ID = UUID.fromString("33333333-3333-3333-3333-000000000001");
    private static final UUID SEEDED_WORD_ID = UUID.fromString("17770000-0000-0000-0000-000000000001");

    /** SCENE_ID가 속한 이야기 (R__1_seed_content.sql). 단어장 화면이 이 값으로 묶음을 그린다. */
    private static final UUID STORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String STORY_TITLE = "방귀 뀌는 며느리";
    private static final String STORY_IMAGE_URL = "/stories/banggui/cover.jpg";

    /**
     * SCENE_ID(R__1_seed_content.sql 장면 1)의 scene_description 원문.
     *
     * <p>문장 구분이 줄바꿈이다 — 화면이 한 문장씩 순차로 보여 주고(장면-05),
     * 사전 렌더 음성의 문장별 타이밍도 이 구분을 따른다.
     */
    private static final String SCENE_DESCRIPTION =
            "옛날 어느 마을에 방귀를 아주 크게 뀌는 며느리가 살았습니다.\n"
                    + "며느리는 시집에 온 뒤로 늘 얌전하고 예의 바르게 보이고 싶었습니다.\n"
                    + "시댁 식구들이 자신을 이상하게 볼까 봐 걱정했기 때문입니다.";

    @Autowired
    private WordbookService wordbookService;

    /** LLM은 실제로 부르지 않는다 — 단어-02 자체는 WordMeaningLlmClientTest가 검증한다. */
    @MockitoBean
    private WordMeaningLlmClient wordMeaningLlmClient;

    @Test
    void 조사가_붙은_단어는_표제어로_저장한다() {
        // "기왓장이"를 눌러도 단어장에는 "기왓장"으로 담긴다. 이야기 어휘
        // 사전(R__4)에 있는 단어라 LLM 없이 검수된 뜻이 붙는다.
        WordResponse saved = wordbookService.create(PARENT_ID, CHILD_ID, new WordCreateRequest(
                "기왓장이", WordEntryType.UNKNOWN, SCENE_ID, null, null));

        assertThat(saved.word()).isEqualTo("기왓장");
        assertThat(saved.meaning()).isEqualTo("지붕을 덮는 납작한 조각");
        verifyNoInteractions(wordMeaningLlmClient);
    }

    @Test
    void 같은_단어를_조사만_다르게_담으면_중복이고_LLM은_부르지_않는다() {
        wordbookService.create(PARENT_ID, CHILD_ID, new WordCreateRequest(
                "기왓장이", WordEntryType.UNKNOWN, SCENE_ID, null, null));

        assertThatThrownBy(() -> wordbookService.create(PARENT_ID, CHILD_ID, new WordCreateRequest(
                "기왓장을", WordEntryType.UNKNOWN, SCENE_ID, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_WORD);
        verifyNoInteractions(wordMeaningLlmClient);
    }

    @Test
    void 이야기_어휘_사전에_있으면_LLM_없이_검수된_뜻으로_저장한다() {
        WordResponse saved = wordbookService.create(PARENT_ID, CHILD_ID, new WordCreateRequest(
                "배나무", WordEntryType.UNKNOWN, SCENE_ID, null, null));

        assertThat(saved.meaning()).isEqualTo("달고 시원한 배가 열리는 나무");
        // 예문 3종(이야기/일상/심화)이 사전에서 함께 온다.
        assertThat(saved.exampleSentence()).isNotBlank();
        assertThat(saved.exampleSentenceDaily()).isNotBlank();
        assertThat(saved.exampleSentenceAdvanced()).isNotBlank();
        verifyNoInteractions(wordMeaningLlmClient);
    }

    @Test
    void 사전_히트여도_요청_예문이_있으면_그쪽을_우선한다() {
        // 아이가 단어를 고른 그 대사 문장이 실려 오는 경로 - 실제로 만난
        // 문장이 사전의 일반 예문보다 낫다.
        WordResponse saved = wordbookService.create(PARENT_ID, CHILD_ID, new WordCreateRequest(
                "친정에", WordEntryType.UNKNOWN, SCENE_ID, null,
                "며느리는 친정 가는 길에 배나무를 보았어요."));

        assertThat(saved.word()).isEqualTo("친정");
        assertThat(saved.exampleSentence()).isEqualTo("며느리는 친정 가는 길에 배나무를 보았어요.");
        verifyNoInteractions(wordMeaningLlmClient);
    }

    @Test
    void 실제_단어가_아니라고_판정되면_저장을_거절한다() {
        // STT 오인식이 만든 존재하지 않는 말이 단어장에 영구히 남으면 안 된다.
        // 동적(LLM 생성) 대사에서 단어를 담는 경로를 여는 전제 조건이다.
        when(wordMeaningLlmClient.generate(any(), any()))
                .thenReturn(new WordMeaningResult(null, null, null, null, false));

        assertThatThrownBy(() -> wordbookService.create(PARENT_ID, CHILD_ID, new WordCreateRequest(
                "방빙끄", WordEntryType.UNKNOWN, SCENE_ID, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_WORD);
    }

    @Test
    void 사전에_없는_단어만_LLM으로_뜻을_만든다() {
        when(wordMeaningLlmClient.generate(eq("아궁이"), any()))
                .thenReturn(new WordMeaningResult("불을 때는 곳이에요.", "아궁이에 불을 지폈어요.", "부엌 아궁이에서 연기가 나요.", "아궁이에 장작을 넣고 불을 지펴 가마솥을 데웠어요.", true));

        WordResponse saved = wordbookService.create(PARENT_ID, CHILD_ID, new WordCreateRequest(
                "아궁이에", WordEntryType.UNKNOWN, SCENE_ID, null, null));

        // 표제어("아궁이")로 정규화된 뒤 LLM에 넘어간다.
        assertThat(saved.word()).isEqualTo("아궁이");
        assertThat(saved.meaning()).isEqualTo("불을 때는 곳이에요.");
    }

    @Test
    void 뜻을_함께_보내면_그대로_저장하고_나머지_예문은_생성으로_채운다() {
        // 예문 3종 규칙(V14) 때문에 뜻이 실려 와도 일상/심화 예문 생성은 필요
        // 하다. 다만 뜻은 사람이 쓴 쪽을 신뢰하고 모델 판정으로 거절하지 않는다.
        when(wordMeaningLlmClient.generate(any(), any()))
                .thenReturn(new WordMeaningResult("모델이 만든 뜻", "이야기 예문",
                        "일상 예문", "심화 예문", true));

        WordResponse saved = wordbookService.create(PARENT_ID, CHILD_ID, new WordCreateRequest(
                "가마솥", WordEntryType.UNKNOWN, SCENE_ID,
                "밥을 짓는 아주 큰 솥이에요.", "부엌에 커다란 가마솥이 걸려 있었습니다."));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.word()).isEqualTo("가마솥");
        assertThat(saved.meaning()).isEqualTo("밥을 짓는 아주 큰 솥이에요.");
        assertThat(saved.exampleSentence()).isEqualTo("부엌에 커다란 가마솥이 걸려 있었습니다.");
        assertThat(saved.exampleSentenceDaily()).isEqualTo("일상 예문");
        assertThat(saved.exampleSentenceAdvanced()).isEqualTo("심화 예문");
        assertThat(saved.sourceSceneId()).isEqualTo(SCENE_ID);
        assertThat(saved.createdAt()).isNotNull();
    }

    @Test
    void 장면_없이도_저장할_수_있다() {
        when(wordMeaningLlmClient.generate(any(), any()))
                .thenReturn(new WordMeaningResult("모델 뜻", "이야기 예문", "일상 예문", "심화 예문", true));

        WordResponse saved = wordbookService.create(PARENT_ID, CHILD_ID, new WordCreateRequest(
                "두레박", WordEntryType.FAVORITE, null, "우물에서 물을 뜨는 그릇이에요.", null));

        assertThat(saved.sourceSceneId()).isNull();
        assertThat(saved.entryType()).isEqualTo(WordEntryType.FAVORITE);
        // 장면이 없으면 이야기도 없다 — 화면은 이 단어를 "이야기 없음" 묶음으로 그린다.
        assertThat(saved.storyId()).isNull();
        assertThat(saved.storyTitle()).isNull();
        assertThat(saved.storyImageUrl()).isNull();
    }

    @Test
    void 장면과_함께_저장하면_이야기_정보가_따라온다() {
        when(wordMeaningLlmClient.generate(any(), any()))
                .thenReturn(new WordMeaningResult("모델 뜻", "이야기 예문", "일상 예문", "심화 예문", true));

        WordResponse saved = wordbookService.create(PARENT_ID, CHILD_ID, new WordCreateRequest(
                "가마솥", WordEntryType.UNKNOWN, SCENE_ID, "밥을 짓는 아주 큰 솥이에요.", null));

        assertThat(saved.storyId()).isEqualTo(STORY_ID);
        assertThat(saved.storyTitle()).isEqualTo(STORY_TITLE);
        assertThat(saved.storyImageUrl()).isEqualTo(STORY_IMAGE_URL);
    }

    @Test
    void 목록에도_이야기_정보가_함께_나온다() {
        WordResponse seeded = wordbookService.getWords(PARENT_ID, CHILD_ID, null).stream()
                .filter(w -> SEEDED_WORD_ID.equals(w.id()))
                .findFirst()
                .orElseThrow();

        assertThat(seeded.storyId()).isEqualTo(STORY_ID);
        assertThat(seeded.storyTitle()).isEqualTo(STORY_TITLE);
    }

    @Test
    void 뜻을_보내지_않으면_LLM이_생성한_뜻과_예문을_저장한다() {
        when(wordMeaningLlmClient.generate("가마솥", SCENE_DESCRIPTION))
                .thenReturn(new WordMeaningResult("음식을 끓이는 큰 솥", "가마솥에서 밥을 지었어요.", "가마솥은 아주 커요.", "명절이면 가마솥 가득 국을 끓였어요.", true));

        WordCreateRequest request = new WordCreateRequest(
                "가마솥", WordEntryType.UNKNOWN, SCENE_ID, null, null);

        WordResponse saved = wordbookService.create(PARENT_ID, CHILD_ID, request);

        assertThat(saved.meaning()).isEqualTo("음식을 끓이는 큰 솥");
        assertThat(saved.exampleSentence()).isEqualTo("가마솥에서 밥을 지었어요.");
    }

    @Test
    void LLM_생성이_실패해도_저장_자체는_막히지_않는다() {
        when(wordMeaningLlmClient.generate("가마솥", SCENE_DESCRIPTION))
                .thenReturn(new WordMeaningResult("지금은 뜻을 알려줄 수 없어요", null, null, null, true));

        WordCreateRequest request = new WordCreateRequest(
                "가마솥", WordEntryType.UNKNOWN, SCENE_ID, null, null);

        WordResponse saved = wordbookService.create(PARENT_ID, CHILD_ID, request);

        assertThat(saved.meaning()).isEqualTo("지금은 뜻을 알려줄 수 없어요");
    }

    @Test
    void 같은_단어를_또_저장하면_거절한다() {
        WordCreateRequest request = new WordCreateRequest(
                "며느리", WordEntryType.UNKNOWN, SCENE_ID, "이미 저장된 단어예요.", null);

        assertThatThrownBy(() -> wordbookService.create(PARENT_ID, CHILD_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_WORD);
    }

    @Test
    void 남의_아이에게는_단어를_저장할_수_없다() {
        WordCreateRequest request = new WordCreateRequest(
                "가마솥", WordEntryType.UNKNOWN, null, "밥을 짓는 큰 솥이에요.", null);

        assertThatThrownBy(() -> wordbookService.create(OTHER_PARENT_ID, CHILD_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }

    @Test
    void 없는_장면을_지정하면_알린다() {
        WordCreateRequest request = new WordCreateRequest(
                "가마솥", WordEntryType.UNKNOWN, UUID.randomUUID(), "밥을 짓는 큰 솥이에요.", null);

        assertThatThrownBy(() -> wordbookService.create(PARENT_ID, CHILD_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void 즐겨찾기는_켜고_끌_수_있다() {
        WordResponse on = wordbookService.toggleFavorite(PARENT_ID, CHILD_ID, SEEDED_WORD_ID);
        assertThat(on.entryType()).isEqualTo(WordEntryType.FAVORITE);

        WordResponse off = wordbookService.toggleFavorite(PARENT_ID, CHILD_ID, SEEDED_WORD_ID);
        assertThat(off.entryType()).isEqualTo(WordEntryType.UNKNOWN);
    }

    @Test
    void 형제의_단어는_즐겨찾기할_수_없다() {
        // 같은 보호자의 다른 아이 경로로 지우의 단어를 건드리는 경우
        assertThatThrownBy(() -> wordbookService.toggleFavorite(PARENT_ID, SIBLING_ID, SEEDED_WORD_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void 형제의_단어는_삭제할_수_없다() {
        assertThatThrownBy(() -> wordbookService.delete(PARENT_ID, SIBLING_ID, SEEDED_WORD_ID))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND);
    }

    @Test
    void 저장한_단어는_목록에_나온다() {
        when(wordMeaningLlmClient.generate(any(), any()))
                .thenReturn(new WordMeaningResult("모델 뜻", "이야기 예문", "일상 예문", "심화 예문", true));

        wordbookService.create(PARENT_ID, CHILD_ID, new WordCreateRequest(
                "가마솥", WordEntryType.UNKNOWN, SCENE_ID, "밥을 짓는 아주 큰 솥이에요.", null));

        assertThat(wordbookService.getWords(PARENT_ID, CHILD_ID, null))
                .extracting(WordResponse::word)
                .contains("가마솥");
    }
}
