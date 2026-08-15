package com.mugunghwa.goodquestion.story.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SceneAudioResolver {

    private final SceneAudioRepository repository;

    /**
     * 이 문장으로 렌더된 공용 음성의 URL. 없으면 null(클라이언트가 실시간 합성한다).
     *
     * @param sceneId 문장이 속한 장면
     * @param text    실제로 내보내는 문장 — 이름 치환까지 끝난 상태여야 한다
     */
    @Transactional(readOnly = true)
    public String urlFor(UUID sceneId, String text) {
        if (sceneId == null || text == null || text.isBlank()) {
            return null;
        }
        String hash = sha256Hex(text);
        return repository.findAllBySceneIdAndChildIdIsNull(sceneId).stream()
                .filter(audio -> matches(hash, audio))
                .findFirst()
                .map(SceneAudioResolver::toUrl)
                .orElse(null);
    }

    /** 장면의 내레이션 음성. 문장별 실측 시각이 필요해 URL만으로는 부족하다. */
    @Transactional(readOnly = true)
    public Optional<SceneAudio> narrationOf(UUID sceneId, String narrationText) {
        if (sceneId == null || narrationText == null || narrationText.isBlank()) {
            return Optional.empty();
        }
        String hash = sha256Hex(narrationText);
        return repository.findBySceneIdAndSlotAndChildIdIsNull(sceneId, SceneAudioSlot.NARRATION)
                .filter(audio -> matches(hash, audio));
    }

    /**
     * 저장 경로를 재생 가능한 URL로.
     *
     * <p>{@code storage_path}는 앞 슬래시 없는 상대 경로로 심겨 있고(예: {@code tts/banggui/...}),
     * 정적 리소스는 루트에서 서빙된다. 이미지가 {@code /stories/...}로 저장돼 있는 것과 모양을 맞춘다.
     */
    private static String toUrl(SceneAudio audio) {
        String path = audio.getStoragePath();
        return path.startsWith("/") || path.startsWith("http") ? path : "/" + path;
    }

    /** text_hash는 char(64)라 값이 짧으면 공백이 붙어 온다. 해시는 정확히 64자지만 방어해 둔다. */
    private static boolean matches(String hash, SceneAudio audio) {
        String stored = audio.getTextHash();
        return stored != null && hash.equalsIgnoreCase(stored.trim());
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
