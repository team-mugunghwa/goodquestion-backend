package com.mugunghwa.goodquestion.learning.wordbook;

import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.learning.wordbook.dto.WordCreateRequest;
import com.mugunghwa.goodquestion.learning.wordbook.dto.WordResponse;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Autowired
    private WordbookService wordbookService;

    @Test
    void 뜻을_함께_보내면_그대로_저장한다() {
        WordResponse saved = wordbookService.create(PARENT_ID, CHILD_ID, new WordCreateRequest(
                "가마솥", WordEntryType.UNKNOWN, SCENE_ID,
                "밥을 짓는 아주 큰 솥이에요.", "부엌에 커다란 가마솥이 걸려 있었습니다."));

        assertThat(saved.id()).isNotNull();
        assertThat(saved.word()).isEqualTo("가마솥");
        assertThat(saved.meaning()).isEqualTo("밥을 짓는 아주 큰 솥이에요.");
        assertThat(saved.sourceSceneId()).isEqualTo(SCENE_ID);
        assertThat(saved.createdAt()).isNotNull();
    }

    @Test
    void 장면_없이도_저장할_수_있다() {
        WordResponse saved = wordbookService.create(PARENT_ID, CHILD_ID, new WordCreateRequest(
                "두레박", WordEntryType.FAVORITE, null, "우물에서 물을 뜨는 그릇이에요.", null));

        assertThat(saved.sourceSceneId()).isNull();
        assertThat(saved.entryType()).isEqualTo(WordEntryType.FAVORITE);
    }

    @Test
    void 뜻을_보내지_않으면_LLM을_타고_아직_미구현이다() {
        WordCreateRequest request = new WordCreateRequest(
                "가마솥", WordEntryType.UNKNOWN, SCENE_ID, null, null);

        assertThatThrownBy(() -> wordbookService.create(PARENT_ID, CHILD_ID, request))
                .isInstanceOf(UnsupportedOperationException.class);
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
        wordbookService.create(PARENT_ID, CHILD_ID, new WordCreateRequest(
                "가마솥", WordEntryType.UNKNOWN, SCENE_ID, "밥을 짓는 아주 큰 솥이에요.", null));

        assertThat(wordbookService.getWords(PARENT_ID, CHILD_ID, null))
                .extracting(WordResponse::word)
                .contains("가마솥");
    }
}
