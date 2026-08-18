package com.mugunghwa.goodquestion.story.freetalk;

import com.mugunghwa.goodquestion.global.vocab.CharacterEmotion;

/** 캐릭터가 말한 한 줄. 음성은 아직 붙지 않았다 - 합성은 트랜잭션 밖의 마지막 단계다. */
public record FreeTalkLine(String text, CharacterEmotion emotion) {
}
