package com.mugunghwa.goodquestion.global.config;

import com.mugunghwa.goodquestion.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 이야기 정적 에셋 서빙 검증.
 *
 * <p>시드의 image_url(/stories/banggui/...)이 실제로 열리는지 본다. 두 가지가 함께
 * 맞아야 한다 - 파일이 classpath:/static 아래 그 경로에 있고, 시큐리티가 인증 없이
 * 통과시킨다. 어느 한쪽이 깨지면 프론트의 모든 장면 이미지가 안 뜬다.
 */
@IntegrationTest
@AutoConfigureMockMvc
class StoryAssetServingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 장면_이미지는_인증_없이_열린다() throws Exception {
        mockMvc.perform(get("/stories/banggui/scenes/01_intro.jpg"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/stories/banggui/cover.jpg"))
                .andExpect(status().isOk());
    }

    @Test
    void 미션과_결과_연출_이미지도_열린다() throws Exception {
        mockMvc.perform(get("/stories/banggui/missions/mission1.jpg"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/stories/banggui/missions/mission2.jpg"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/stories/banggui/scenes/07_result.jpg"))
                .andExpect(status().isOk());
    }

    /** 시드가 참조하는 장면 이미지 9장이 전부 있어야 한다. 하나라도 빠지면 그 장면만 깨진다. */
    @Test
    void 시드가_참조하는_장면_이미지_아홉_장이_전부_있다() throws Exception {
        String[] files = {"01_intro", "02_holding", "03_dialogue1", "04_bigfart",
                "05_dialogue2", "06_peartree", "07_dialogue3", "08_apology", "09_dialogue4"};
        for (String file : files) {
            mockMvc.perform(get("/stories/banggui/scenes/" + file + ".jpg"))
                    .andExpect(status().isOk());
        }
    }
}
