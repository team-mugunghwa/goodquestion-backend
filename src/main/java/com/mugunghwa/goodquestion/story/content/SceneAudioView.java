package com.mugunghwa.goodquestion.story.content;

import java.util.List;

/**
 * 사전 렌더 음성의 읽기 전용 뷰. 캐시와 응답 조립이 이것만 본다.
 *
 * <p>엔티티를 캐시에 담지 않는 이유: 캐시에 담긴 순간 영속성 컨텍스트 밖에서 여러 요청이
 * 같은 인스턴스를 공유한다. LAZY 프록시가 섞이면 초기화 시점을 통제할 수 없고, 누가
 * 실수로 값을 바꾸면 모든 요청에 번진다. 값만 복사해 두면 둘 다 원천적으로 불가능하다.
 */
public record SceneAudioView(SceneAudioSlot slot, String url, String textHash,
                             List<SceneAudio.SentenceTiming> sentenceTimings) {

    /** text_hash는 char(64)라 값이 짧으면 공백이 붙어 온다. 여기서 정리해 비교를 단순하게 한다. */
    static SceneAudioView from(SceneAudio audio) {
        List<SceneAudio.SentenceTiming> timings = audio.getSentenceTimings();
        return new SceneAudioView(
                audio.getSlot(), audio.url(),
                audio.getTextHash() == null ? null : audio.getTextHash().trim(),
                timings == null ? List.of() : List.copyOf(timings));
    }
}
