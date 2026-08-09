package com.mugunghwa.goodquestion.ai.tts;

public interface TtsClient {

    /**
     * 텍스트를 합성해 재생 가능한 오디오 URL을 돌려준다.
     *
     * @param characterName 캐릭터 보이스 매핑용. null이면 내레이션 보이스.
     * @return 만료 시각이 있는 오디오 URL
     */
    SynthesizedAudio synthesize(String text, String characterName);
}
