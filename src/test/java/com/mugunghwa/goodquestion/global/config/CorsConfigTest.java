package com.mugunghwa.goodquestion.global.config;

import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * preflight(OPTIONS)가 인증 벽에 막히지 않는지 검사한다.
 *
 * <p>이 테스트가 없으면 회귀를 못 잡는다. SecurityConfig 에서 {@code .cors()} 한 줄이
 * 빠져도 curl 과 서버 로그는 멀쩡해 보이고, 브라우저에서만 조용히 전부 실패한다.
 */
@IntegrationTest
@AutoConfigureMockMvc
class CorsConfigTest {

    private static final String PROD = "https://goodquestion-frontend.vercel.app";
    private static final String PREVIEW =
            "https://goodquestion-frontend-a1b2c3d4-team-mugunghwa.vercel.app";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 운영_오리진의_preflight를_통과시킨다() throws Exception {
        mockMvc.perform(options("/api/stories")
                        .header("Origin", PROD)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", PROD));
    }

    /**
     * 인증이 필요한 경로여도 preflight 는 401 이 아니어야 한다. 브라우저는 preflight 에
     * Authorization 헤더를 붙이지 않으므로, 여기서 401 이 나면 본 요청은 보내지도 않는다.
     */
    @Test
    void 인증이_필요한_경로의_preflight도_401이_아니다() throws Exception {
        mockMvc.perform(options("/api/children")
                        .header("Origin", PROD)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk());
    }

    /** Vercel 프리뷰는 커밋마다 주소가 바뀌므로 와일드카드 패턴으로 잡아야 한다. */
    @Test
    void 프리뷰_배포_오리진도_통과시킨다() throws Exception {
        mockMvc.perform(options("/api/stories")
                        .header("Origin", PREVIEW)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", PREVIEW));
    }

    /** 포트가 매번 바뀌는 flutter run -d chrome 을 위해 로컬은 전 포트를 연다. */
    @Test
    void 로컬_개발_오리진은_포트가_달라도_통과시킨다() throws Exception {
        mockMvc.perform(options("/api/stories")
                        .header("Origin", "http://localhost:53421")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk());
    }

    /**
     * 팀 슬러그를 패턴에 넣은 이유. 같은 이름으로 프로젝트를 만든 남의 Vercel 계정은
     * 슬러그가 달라 걸러진다. 이 테스트가 깨지면 패턴이 너무 넓어진 것이다.
     */
    @Test
    void 다른_팀의_Vercel_주소는_거부한다() throws Exception {
        mockMvc.perform(options("/api/stories")
                        .header("Origin", "https://goodquestion-frontend-a1b2c3d4-attacker.vercel.app")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 관계없는_오리진은_거부한다() throws Exception {
        mockMvc.perform(options("/api/stories")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
