package com.mugunghwa.goodquestion.ai.tts;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 합성 결과 캐시. 라우터 위에 얹어 벤더와 무관하게 한 벌만 둔다.
 *
 * <p>원래는 벤더 클라이언트 3개가 각자 같은 캐시 코드를 들고 있었다. 라우터가 요청마다
 * 벤더 하나를 골라 위임하므로 <b>어느 시점에도 한 벌만 일하고 나머지 둘은 비어 있었다.</b>
 * 캐시가 벤더의 관심사가 아니라 "합성"이라는 행위의 관심사였던 것이다.
 *
 * <p>키에 벤더를 넣는다. 벤더마다 같은 문장의 소리가 다르므로 섞이면 안 되고,
 * 넣어 두면 벤더를 되돌렸을 때 옛 캐시가 그대로 살아 있다.
 *
 * <p><b>상한은 개수가 아니라 바이트다.</b> 값이 base64 data URL이라 문장 길이에 따라
 * 37KB에서 176KB까지 벌어진다(실측). 개수로 512를 잡으면 힙 점유가 19MB일 수도
 * 90MB일 수도 있어 예측이 안 된다. 예산을 바이트로 주면 무엇이 들어오든 그 안이다.
 *
 * <p>예산이 작은 이유. 도입 당시에는 고정 대사 20개를 반복 과금에서 막는 것이 목적이었는데,
 * 그 대사들은 이제 사전 렌더(scene_audio)가 정적 파일로 서빙한다. 지금 여기 남는 것은
 * LLM이 매 턴 새로 만드는 대사(재사용 거의 없음)와 아이 이름이 치환된 대사(그 아이가
 * 다시 들을 때 적중)뿐이라, 512칸을 잡고 있을 이유가 없다.
 */
@Slf4j
@Component
@Primary
public class CachingTtsClient implements TtsClient {

    private final RoutingTtsClient delegate;
    private final Cache<String, SynthesizedAudio> cache;

    public CachingTtsClient(RoutingTtsClient delegate,
                            @Value("${external.tts.cache-budget-bytes:8388608}") long budgetBytes) {
        this.delegate = delegate;
        this.cache = Caffeine.newBuilder()
                .maximumWeight(budgetBytes)
                .weigher((String key, SynthesizedAudio audio) -> weigh(audio))
                .build();
    }

    @Override
    public SynthesizedAudio synthesize(String text, String characterName) {
        String key = delegate.currentVendor().name() + " " + characterName + " " + text;
        // get(key, loader)는 같은 키의 동시 미스를 한 번만 계산한다. 이전의 get 후 put은
        // 원자적이지 않아 겹친 요청이 벤더를 중복 호출할 수 있었다(과금이 걸린 호출이다).
        return cache.get(key, ignored -> delegate.synthesize(text, characterName));
    }

    /** data URL 문자열 길이를 그대로 무게로 쓴다. base64는 ASCII라 문자 수가 곧 바이트 수다. */
    private static int weigh(SynthesizedAudio audio) {
        String url = audio.audioUrl();
        return url == null ? 0 : url.length();
    }

    /**
     * 밀린 축출을 지금 수행한다. 테스트 전용이다.
     *
     * <p>Caffeine은 축출을 쓰기 시점에 동기로 하지 않고 뒤로 미룬다. 운영에서는 그게 맞지만
     * (쓰기 경로가 빨라진다) 예산 초과 후 상태를 확인하려는 테스트는 결정적이지 않게 된다.
     */
    void cleanUp() {
        cache.cleanUp();
    }
}
