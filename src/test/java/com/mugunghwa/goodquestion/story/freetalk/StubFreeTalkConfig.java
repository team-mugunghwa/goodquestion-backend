package com.mugunghwa.goodquestion.story.freetalk;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 자유 대화 테스트의 대역 설정. LLM과 TTS만 바꾸고 나머지는 실제 경로를 탄다.
 *
 * <p><b>테스트 클래스 안에 중첩하지 않고 따로 둔다.</b> 중첩 {@code @TestConfiguration}은
 * 그 테스트 클래스의 컨텍스트 구성에 직접 들어가고 {@code @Import}는 커스터마이저로
 * 들어가서, 같은 설정을 써도 컨텍스트 키가 달라진다. 그러면 컨텍스트가 하나 더 뜨고
 * 컨텍스트마다 붙는 커넥션 풀이 PostgreSQL의 max_connections를 넘겨, 이 기능과 무관한
 * 테스트가 "too many clients"로 죽는다(실제로 겪었다). 두 클래스가 똑같이
 * {@code @Import(StubFreeTalkConfig.class)}만 쓰면 컨텍스트는 하나다.
 */
@TestConfiguration
class StubFreeTalkConfig {

    static final String STUB_AUDIO_URL = "https://audio.test/free-talk.mp3";

    @Bean
    @Primary
    StubFreeTalkLlmClient stubFreeTalkLlmClient() {
        return new StubFreeTalkLlmClient();
    }

    /** 벤더를 부르지 않는다. 합성 성공 경로만 있으면 되고 실패 경로는 별도 관심사다. */
    @Bean
    @Primary
    StubFreeTalkVoice stubFreeTalkVoice() {
        return new StubFreeTalkVoice();
    }
}
