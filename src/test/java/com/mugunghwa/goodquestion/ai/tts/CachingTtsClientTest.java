package com.mugunghwa.goodquestion.ai.tts;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 합성 캐시의 동작을 벤더 없이 확인한다.
 *
 * <p>라우터를 상속해 벤더 호출만 세는 대역으로 바꾼다 - 실제 벤더를 부르면 과금이 나가고
 * 네트워크에 의존하게 된다. 캐시 자체는 벤더가 무엇인지 모르므로 이 대역으로 충분하다.
 */
class CachingTtsClientTest {

    /** 넉넉한 예산. 축출을 보려는 테스트만 따로 좁힌 예산을 쓴다. */
    private static final long WIDE_BUDGET = 8L * 1024 * 1024;

    @Test
    void 같은_문장은_벤더를_한_번만_부른다() {
        CountingRouter router = new CountingRouter();
        CachingTtsClient client = new CachingTtsClient(router, WIDE_BUDGET);

        client.synthesize("안녕", "며느리");
        client.synthesize("안녕", "며느리");
        client.synthesize("안녕", "며느리");

        assertThat(router.calls.get()).isEqualTo(1);
    }

    @Test
    void 캐릭터가_다르면_따로_합성한다() {
        CountingRouter router = new CountingRouter();
        CachingTtsClient client = new CachingTtsClient(router, WIDE_BUDGET);

        client.synthesize("안녕", "며느리");
        client.synthesize("안녕", "시아버지");

        assertThat(router.calls.get()).isEqualTo(2);
    }

    /**
     * 벤더가 바뀌면 다시 합성한다.
     *
     * <p>키에 벤더가 없으면 관리자가 벤더를 바꿔도 옛 벤더의 소리가 캐시에서 계속 나간다.
     * 전환이 안 먹은 것처럼 보이는데 로그에는 아무 흔적이 없어 추적이 어렵다.
     */
    @Test
    void 벤더가_바뀌면_다시_합성한다() {
        CountingRouter router = new CountingRouter();
        CachingTtsClient client = new CachingTtsClient(router, WIDE_BUDGET);

        client.synthesize("안녕", "며느리");
        router.vendor = TtsVendor.CHIRP3;
        client.synthesize("안녕", "며느리");

        assertThat(router.calls.get()).isEqualTo(2);
    }

    /**
     * 상한은 개수가 아니라 바이트다.
     *
     * <p>값이 base64 data URL이라 문장 길이에 따라 크기가 몇 배씩 벌어진다. 개수로 상한을
     * 두면 힙 점유가 예측되지 않는다. 예산을 넘기면 축출이 일어나 캐시가 무한히 자라지 않는다.
     */
    @Test
    void 바이트_예산을_넘기면_축출된다() {
        CountingRouter router = new CountingRouter();
        // 응답 하나가 약 1KB다. 3KB 예산이면 열 문장을 다 들고 있을 수 없다.
        CachingTtsClient client = new CachingTtsClient(router, 3 * 1024);

        for (int i = 0; i < 10; i++) {
            client.synthesize("문장" + i, "며느리");
        }
        client.cleanUp();

        // 처음 것들은 밀려났으므로 다시 부르면 벤더를 또 탄다.
        for (int i = 0; i < 10; i++) {
            client.synthesize("문장" + i, "며느리");
        }

        assertThat(router.calls.get()).isGreaterThan(10);
    }

    /**
     * 같은 키로 동시에 들어와도 벤더는 한 번만 부른다.
     *
     * <p>이전 구현은 get 후 put이라 원자적이지 않았다. 겹친 요청이 둘 다 미스를 보고
     * 각자 벤더를 불렀는데, 이건 과금이 걸린 호출이다.
     */
    @Test
    void 같은_키의_동시_요청은_벤더를_한_번만_부른다() throws Exception {
        CountingRouter router = new CountingRouter();
        router.delayMillis = 50;
        CachingTtsClient client = new CachingTtsClient(router, WIDE_BUDGET);

        int threads = 4;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        List<Thread> workers = java.util.stream.IntStream.range(0, threads)
                .mapToObj(i -> new Thread(() -> {
                    try {
                        barrier.await();
                        client.synthesize("같은 문장", "며느리");
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }))
                .toList();

        workers.forEach(Thread::start);
        for (Thread worker : workers) {
            worker.join();
        }

        assertThat(router.calls.get()).isEqualTo(1);
    }

    /** 벤더 호출 횟수만 세는 라우터. 실제 합성은 하지 않는다. */
    private static class CountingRouter extends RoutingTtsClient {

        final AtomicInteger calls = new AtomicInteger();
        TtsVendor vendor = TtsVendor.OPENAI;
        long delayMillis = 0;

        CountingRouter() {
            super(null, null, null, null, "openai");
        }

        @Override
        TtsVendor currentVendor() {
            return vendor;
        }

        @Override
        public SynthesizedAudio synthesize(String text, String characterName) {
            calls.incrementAndGet();
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            // 약 1KB짜리 응답. 무게 계산이 URL 길이를 쓰므로 길이만 맞추면 된다.
            return new SynthesizedAudio("data:audio/mp3;base64," + "A".repeat(1024), null);
        }
    }

}
