package com.mugunghwa.goodquestion.story.content;

/** 한 장면에서 음성이 붙는 자리. DB의 {@code scene_audio.slot} 체크 제약과 같다. */
public enum SceneAudioSlot {

    /** STORY 장면의 내레이션 — {@code scene_description} 낭독 */
    NARRATION,

    /** DIALOGUE 장면의 고정 첫 대사 */
    OPENING,

    /** DIALOGUE 장면의 고정 마지막 대사 */
    CLOSING
}
