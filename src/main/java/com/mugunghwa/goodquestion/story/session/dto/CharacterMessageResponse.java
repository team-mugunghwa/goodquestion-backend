package com.mugunghwa.goodquestion.story.session.dto;

import com.mugunghwa.goodquestion.story.content.SceneAudioView;
import com.mugunghwa.goodquestion.story.session.Message;

import java.util.List;
import java.util.UUID;

/**
 * 명세 3-9 캐릭터 메시지. text는 "ㅇㅇ" 이름 치환이 끝난 상태다(캐릭터-17).
 * audioUrl이 null이면 클라이언트가 텍스트→음성 변환을 호출한다.
 */
public record CharacterMessageResponse(UUID messageId, String text, String audioUrl,
                                       List<AudioTiming> audioTimings) {

    /**
     * 사전 렌더 음성 안에서 문장 하나가 차지하는 구간(초).
     *
     * <p>화면은 대사를 문장 단위로 넘긴다. 사전 렌더는 대사 전체가 파일 하나라, 이 값이 없으면
     * 클라이언트가 "파일 하나를 통째로 재생하고 자막은 안 넘기거나 / 문장별로 다시 합성하거나"
     * 둘 중 하나를 골라야 한다. 실제로 후자를 골라서, 두 문장 이상인 대사는 사전 렌더가
     * 통째로 무시되고 매번 다시 합성되고 있었다.
     *
     * <p>문장마다 따로 합성해 이어 붙이며 잰 실측값이다 — 글자수 비례 추정이 아니다.
     * index는 마침표·물음표·느낌표로 자른 문장 순서와 같다.
     */
    public record AudioTiming(int index, double start, double end) {
    }

    /**
     * 사전 렌더 음성이 없는 메시지 — LLM이 그때그때 만든 대사는 미리 렌더할 수 없다.
     * 클라이언트가 {@code /api/tts}로 합성한다.
     */
    public static CharacterMessageResponse from(Message message) {
        return from(message, null);
    }

    /**
     * @param audio 이 문장으로 렌더된 음성. null이면 클라이언트가 합성한다.
     *              {@code SceneAudioResolver}가 <b>문장 해시로</b> 찾아 준 것만 넣어야 한다 —
     *              슬롯만 보고 넣으면 대사를 고쳤을 때 옛 음성이 그대로 나간다.
     */
    public static CharacterMessageResponse from(Message message, SceneAudioView audio) {
        return new CharacterMessageResponse(message.getId(), message.getText(),
                audio == null ? null : audio.url(),
                audio == null ? List.of() : toTimings(audio));
    }

    private static List<AudioTiming> toTimings(SceneAudioView audio) {
        return audio.sentenceTimings().stream()
                .map(t -> new AudioTiming(t.index(), t.start(), t.end()))
                .toList();
    }
}
