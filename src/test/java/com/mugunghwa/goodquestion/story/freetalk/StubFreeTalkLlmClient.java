package com.mugunghwa.goodquestion.story.freetalk;

import com.mugunghwa.goodquestion.ai.freetalk.FreeTalkLlmClient;
import com.mugunghwa.goodquestion.global.vocab.CharacterEmotion;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

/**
 * 자유 대화 대사 대역.
 *
 * <p>어떤 단계로 불렸는지를 남긴다 - 대사 내용이 아니라 <b>단계</b>가 검증 대상이다.
 * 마지막 턴에 마무리 지시가 갔는지는 그것으로만 확인할 수 있다.
 *
 * <p>실패 스위치와 장벽도 여기 있다. 벤더 타임아웃 자리와 동시 출발 자리가 둘 다 이
 * 호출이라, 대역을 나누면 같은 자리를 두 번 흉내 내게 된다.
 */
class StubFreeTalkLlmClient extends FreeTalkLlmClient {

    String lastStage;
    volatile int calls;
    private boolean failNext;

    /**
     * 대사 생성 자리에서 스레드를 모았다 함께 내보낸다. 준비(읽기)와 저장(쓰기) 사이가
     * 바로 이 자리라, 여기서 함께 출발시켜야 겹친 요청이 같은 턴 수를 읽은 상태로
     * 저장에 뛰어든다. 기본값 1은 그냥 지나간다.
     */
    private volatile CyclicBarrier barrier = new CyclicBarrier(1);

    StubFreeTalkLlmClient() {
        super(null, null);
    }

    void reset() {
        lastStage = null;
        calls = 0;
        failNext = false;
        barrier = new CyclicBarrier(1);
    }

    /** 다음 한 번만 실패한다. 벤더 타임아웃 자리를 재현한다. */
    void willFail() {
        failNext = true;
    }

    void releaseTogether(int parties) {
        this.barrier = new CyclicBarrier(parties);
    }

    @Override
    public FreeTalkLlmResult speak(FreeTalkLlmInput input) {
        lastStage = input.stage();
        calls++;
        if (failNext) {
            failNext = false;
            throw new IllegalStateException("대역이 대사 생성을 실패시킨다");
        }
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("동시 출발에 실패했다", e);
        }
        return new FreeTalkLlmResult("그때 이야기 말이지, 나도 자주 생각나.",
                CharacterEmotion.HAPPY.name());
    }
}
