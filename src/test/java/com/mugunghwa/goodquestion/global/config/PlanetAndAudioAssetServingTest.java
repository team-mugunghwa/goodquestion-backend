package com.mugunghwa.goodquestion.global.config;

import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 사전 렌더 음성과 행성 아이템 모델의 서빙 검증.
 *
 * <p>파일이 {@code classpath:/static} 아래 있는 것만으로는 부족하다 - 시큐리티가 인증 없이
 * 통과시켜야 한다. {@code <audio src>}와 GLTF 로더는 이미지 태그와 마찬가지로 Authorization
 * 헤더를 못 싣고, 행성은 별도 웹앱이라 토큰을 들고 있지도 않다.
 *
 * <p>두 경로 모두 permitAll 이 빠져 있어 401이 나갔다. 파일을 넣는 것과 여는 것이 다른
 * 파일에 흩어져 있어 한쪽만 하고 넘어가기 쉬운데, 그러면 화면에서 소리와 모델만 조용히 빠진다.
 */
@IntegrationTest
@AutoConfigureMockMvc
class PlanetAndAudioAssetServingTest {

    @Autowired
    private MockMvc mockMvc;

    /** 장면 음성 13개(내레이션 5 + 고정 첫/마지막 대사 8). scene_audio의 storage_path와 같은 경로다. */
    @Test
    void 장면_음성이_인증_없이_전부_열린다() throws Exception {
        String[] narrations = {"01", "02", "04", "06", "08"};
        for (String order : narrations) {
            mockMvc.perform(get("/tts/banggui/sc_banggui_" + order + ".mp3"))
                    .andExpect(status().isOk());
        }
        String[] dialogues = {"03", "05", "07", "09"};
        for (String order : dialogues) {
            mockMvc.perform(get("/tts/banggui/sc_banggui_" + order + "_opening.mp3"))
                    .andExpect(status().isOk());
            mockMvc.perform(get("/tts/banggui/sc_banggui_" + order + "_closing.mp3"))
                    .andExpect(status().isOk());
        }
    }

    /**
     * 행성 아이템 모델. 클라이언트는 자기 카탈로그를 model_url 경로로 매칭하므로,
     * 열리지 않으면 상점에 이름만 뜨고 아무것도 그려지지 않는다.
     */
    @Test
    void 아이템_모델이_인증_없이_열린다() throws Exception {
        mockMvc.perform(get("/items/models/rock_smallA.glb")).andExpect(status().isOk());
        mockMvc.perform(get("/items/models/forest/stones.glb")).andExpect(status().isOk());
        mockMvc.perform(get("/items/models/city/building-type-a.glb")).andExpect(status().isOk());
        mockMvc.perform(get("/items/models/pets/animal-bunny.glb")).andExpect(status().isOk());
    }
}
