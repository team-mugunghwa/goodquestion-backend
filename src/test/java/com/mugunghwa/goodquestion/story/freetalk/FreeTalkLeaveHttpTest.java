package com.mugunghwa.goodquestion.story.freetalk;

import com.mugunghwa.goodquestion.global.security.JwtProvider;
import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "바로 나가기"의 <b>HTTP 계약</b>을 못박는다 — 경로 · 메서드 · 204 · 인증.
 *
 * <p>나머지 자유 대화 테스트는 서비스를 직접 부른다. 그래서 컨트롤러가 어느 경로에
 * 붙어 있는지, 무엇을 돌려주는지는 <b>어느 테스트도 재지 않았다</b>. 이 계약은
 * 프런트가 {@code /free-talk/{id}/leave} 로 부르는 쪽과 맞물려야만 기능이 도는데,
 * 한쪽만 고쳐도 컴파일은 통과하고 테스트도 통과한다 — 앱에서만 조용히 404가 된다.
 *
 * <p>애노테이션 둘은 골라 붙인 것이다. {@code @Transactional} 이 없으면 여기서 심은
 * 대화가 그대로 남아 뒤따르는 인물 목록 테스트의 결과를 바꾼다. {@code StubFreeTalkConfig}
 * 는 <b>일부러 붙이지 않았다</b> — 이 경로는 LLM·TTS 를 타지 않는데, 붙이면 컨텍스트 키가
 * 갈려 스프링 컨텍스트가 하나 더 뜨고 커넥션 풀이 PostgreSQL max_connections 를 넘긴다
 * (무관한 PasswordResetServiceTest 가 "too many clients" 로 죽는다. 실측했다).
 *
 * <p>같은 이유로 상태코드까지 잰다. 프런트는 본문을 파싱하지 않는 전제로
 * ({@code parse: (_) {}}) 부르므로, 여기가 200에 본문을 실어 보내기 시작하면
 * 그 전제가 깨진다.
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class FreeTalkLeaveHttpTest {

    private static final UUID PARENT_ID = UUID.fromString("99999999-9999-9999-9999-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String token;

    @BeforeEach
    void setUp() {
        token = jwtProvider.issue(PARENT_ID);
    }

    /** 남의 대화를 닫을 수 있으면 안 된다. 토큰 없이는 들어오지 못한다. */
    @Test
    void 인증_없이_부르면_거부된다() throws Exception {
        mockMvc.perform(post("/api/free-talk/{id}/leave", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 열린_대화를_닫고_본문_없이_204를_돌려준다() throws Exception {
        UUID freeTalkId = openTalk();

        mockMvc.perform(post("/api/free-talk/{id}/leave", freeTalkId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEmpty());

        assertThat(endedAt(freeTalkId)).isNotNull();
    }

    /** 다시 눌러도 안전해야 한다 — 프런트는 응답을 기다리지 않으므로 재시도가 겹칠 수 있다. */
    @Test
    void 이미_닫힌_대화에_또_불러도_204다() throws Exception {
        UUID freeTalkId = openTalk();
        leave(freeTalkId);

        mockMvc.perform(post("/api/free-talk/{id}/leave", freeTalkId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    private void leave(UUID freeTalkId) throws Exception {
        mockMvc.perform(post("/api/free-talk/{id}/leave", freeTalkId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    /**
     * 대화 한 건을 직접 심는다. 서비스로 열면 첫 인사 LLM을 타는데, 이 테스트가
     * 재려는 것은 대사가 아니라 HTTP 계약이라 그 왕복이 끼어들 이유가 없다.
     */
    private UUID openTalk() {
        UUID freeTalkId = UUID.randomUUID();
        jdbcTemplate.update("""
                insert into free_talks (id, child_id, story_id, character_id, turn_count)
                select ?, c.id, ch.story_id, ch.id, 0
                from children c, characters ch
                where c.parent_id = ? and ch.story_id = (select story_id from characters limit 1)
                limit 1
                """, freeTalkId, PARENT_ID);
        return freeTalkId;
    }

    private Object endedAt(UUID freeTalkId) {
        return jdbcTemplate.queryForObject(
                "select ended_at from free_talks where id = ?", Object.class, freeTalkId);
    }
}
