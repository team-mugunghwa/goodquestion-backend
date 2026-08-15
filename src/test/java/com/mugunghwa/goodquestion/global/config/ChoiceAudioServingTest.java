package com.mugunghwa.goodquestion.global.config;

import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 선택지 음성 서빙 검증.
 *
 * <p>음성 인식이 세 번 실패하면 화면이 선택지로 내려간다. 그때 재생할 음성을 미리 렌더해
 * 정적 자산으로 두었고, 매니페스트가 어떤 상황에 어느 파일을 쓸지 담고 있다.
 *
 * <p>매니페스트를 읽어 그 안의 파일이 <b>전부</b> 열리는지 본다 - 하나라도 빠지면 그 상황에서만
 * 소리가 안 나는데, 세 번 실패해야 도달하는 화면이라 손으로는 좀처럼 발견되지 않는다.
 */
@IntegrationTest
@AutoConfigureMockMvc
class ChoiceAudioServingTest {

    private static final String MANIFEST = "static/tts/banggui/choices/manifest.json";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 매니페스트가_가리키는_선택지_음성이_전부_열린다() throws Exception {
        JsonNode manifest = readManifest();
        List<String> urls = new ArrayList<>();
        for (JsonNode item : manifest.get("items")) {
            urls.add(item.get("url").asString());
        }

        assertThat(urls).hasSize(52);   // 장면 4개 × 13개(1턴차 3 + 2턴차 3 + 요소별 5 + 안내 2)
        for (String url : urls) {
            mockMvc.perform(get(url)).andExpect(status().isOk());
        }
    }

    @Test
    void 선택지_음성은_인증_없이_열린다() throws Exception {
        mockMvc.perform(get("/tts/banggui/choices/manifest.json")).andExpect(status().isOk());
        mockMvc.perform(get("/tts/banggui/choices/c3_t1_1.mp3")).andExpect(status().isOk());
    }

    /** 대화 장면 넷이 각각 자기 캐릭터 목소리를 쓴다 - 나레이터가 끼어들면 사람이 바뀐 것처럼 들린다. */
    @Test
    void 장면마다_캐릭터_보이스가_지정돼_있다() throws Exception {
        JsonNode voices = readManifest().get("sceneVoices");

        assertThat(voices.get("3").asString()).isEqualTo("Leda");    // 며느리
        assertThat(voices.get("5").asString()).isEqualTo("Puck");    // 시아버지
        assertThat(voices.get("7").asString()).isEqualTo("Charon");  // 이장
        assertThat(voices.get("9").asString()).isEqualTo("Leda");    // 며느리
    }

    private JsonNode readManifest() throws Exception {
        try (InputStream in = new ClassPathResource(MANIFEST).getInputStream()) {
            return new ObjectMapper().readTree(in);
        }
    }
}
