package com.mugunghwa.goodquestion.story.freetalk;

import com.mugunghwa.goodquestion.ai.tts.TtsClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 자유 대화 대사의 음성 합성. 기존 TTS 경로(RoutingTtsClient)를 그대로 탄다 -
 * 캐릭터별 보이스가 이미 이름으로 매핑돼 있어 여기서 고를 것이 없다.
 *
 * <p><b>트랜잭션 밖에서 부른다.</b> 벤더 왕복이 수 초라 트랜잭션 안에 두면 그동안
 * DB 커넥션을 쥐게 된다(docs/트러블슈팅_턴_처리_커넥션_점유.md와 같은 이유).
 *
 * <p>합성 실패는 대화를 끊지 않는다. 대사는 이미 만들어졌고 화면에는 글로 나가므로,
 * 목소리 하나 때문에 아이가 방금 한 말이 통째로 사라지는 쪽이 더 나쁘다. null을 주면
 * 클라이언트가 학습 대화와 같은 방식으로 /api/tts를 부른다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FreeTalkVoice {

    private final TtsClient ttsClient;

    /** @return 재생 가능한 오디오 URL. 합성에 실패하면 null */
    public String synthesize(String text, String characterName) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return ttsClient.synthesize(text, characterName).audioUrl();
        } catch (RuntimeException e) {
            log.warn("자유 대화 음성 합성 실패 character={} - 텍스트만 내린다", characterName, e);
            return null;
        }
    }
}
