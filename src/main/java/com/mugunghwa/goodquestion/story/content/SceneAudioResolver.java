package com.mugunghwa.goodquestion.story.content;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * 지금 내보낼 문장에 맞는 사전 렌더 음성을 찾아 준다.
 *
 * <p><b>슬롯이 아니라 텍스트 해시로 고른다.</b> 슬롯으로 고르면 대사를 고쳤을 때 옛 음성이
 * 그대로 나가고 아무도 눈치채지 못한다. 해시로 고르면 어긋난 순간 그냥 안 잡히고,
 * 클라이언트는 평소대로 {@code /api/tts}로 합성해 재생한다 — 화면과 소리가 갈리는 대신
 * 사전 렌더의 이점만 잃는다.
 *
 * <p>덕분에 아이 이름이 들어간 대사도 알아서 걸러진다. 공용 음성은 "ㅇㅇ아"를 읽을 수 없는데,
 * 치환된 문장("민준아, ...")의 해시는 렌더 원본과 다르므로 매칭되지 않는다.
 *
 * <p>후보 목록은 {@link SceneAudioCache}가 들고 있다 - DB를 치지 않으므로 트랜잭션도 없다.
 * 해시 대조는 여기 남는다. 캐시는 "이 장면에 어떤 음성이 있는가"까지만 알고,
 * "지금 이 문장에 써도 되는가"는 매번 새로 판정해야 위의 성질이 유지된다.
 */
@Component
@RequiredArgsConstructor
public class SceneAudioResolver {

    private final SceneAudioCache cache;

    /**
     * 이 문장으로 렌더된 공용 음성. 없으면 비어 있다.
     *
     * <p>URL만으로는 부족한 호출자가 있다 - 캐릭터 메시지는 문장별 실측 시각까지 실어야
     * 클라이언트가 파일 하나를 재생하면서 자막을 문장 단위로 넘길 수 있다.
     *
     * @param sceneId 문장이 속한 장면
     * @param text    실제로 내보내는 문장 — 이름 치환까지 끝난 상태여야 한다
     */
    public Optional<SceneAudioView> forText(UUID sceneId, String text) {
        if (sceneId == null || text == null || text.isBlank()) {
            return Optional.empty();
        }
        String hash = sha256Hex(text);
        return cache.sharedAudioOf(sceneId).stream()
                .filter(audio -> matches(hash, audio))
                .findFirst();
    }

    /** 장면의 내레이션 음성. 문장별 실측 시각이 필요해 URL만으로는 부족하다. */
    public Optional<SceneAudioView> narrationOf(UUID sceneId, String narrationText) {
        if (sceneId == null || narrationText == null || narrationText.isBlank()) {
            return Optional.empty();
        }
        String hash = sha256Hex(narrationText);
        return cache.sharedAudioOf(sceneId).stream()
                .filter(audio -> audio.slot() == SceneAudioSlot.NARRATION)
                .filter(audio -> matches(hash, audio))
                .findFirst();
    }

    private static boolean matches(String hash, SceneAudioView audio) {
        return audio.textHash() != null && hash.equalsIgnoreCase(audio.textHash());
    }

    static String sha256Hex(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 JRE가 반드시 제공한다. 여기 오면 런타임이 망가진 것이다.
            throw new IllegalStateException("SHA-256을 쓸 수 없습니다", e);
        }
    }
}
