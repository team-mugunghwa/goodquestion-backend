package com.mugunghwa.goodquestion.story.freetalk;

/**
 * 자유 대화 음성 합성 대역. 벤더를 부르지 않고 상수 URL만 돌려준다.
 *
 * <p><b>횟수를 센다.</b> "나가기가 TTS를 부르지 않는다"가 그 기능의 존재 이유인데,
 * 응답에 audioUrl 필드가 없다는 것만으로는 증거가 되지 못한다 - 합성해 놓고 응답에
 * 싣지 않았을 뿐일 수도 있다. 부른 횟수를 세야 벤더 요금이 안 나간다는 말이 성립한다.
 *
 * <p>익명 클래스가 아니라 이름 있는 클래스로 둔 것은 대역을 타입으로 주입받기 위해서다
 * ({@link StubFreeTalkLlmClient}와 같은 방식). 설정 클래스는 여전히
 * {@link StubFreeTalkConfig} 하나뿐이라 스프링 컨텍스트는 하나로 유지된다.
 */
class StubFreeTalkVoice extends FreeTalkVoice {

    volatile int calls;

    StubFreeTalkVoice() {
        super(null);
    }

    void reset() {
        calls = 0;
    }

    @Override
    public String synthesize(String text, String characterName) {
        calls++;
        return StubFreeTalkConfig.STUB_AUDIO_URL;
    }
}
