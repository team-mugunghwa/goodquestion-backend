package com.mugunghwa.goodquestion.story.content;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SceneAudioRepository extends JpaRepository<SceneAudio, UUID> {

    /** 장면의 공용 음성 전부(장면당 최대 3건 — 내레이션/첫 대사/마지막 대사). */
    List<SceneAudio> findAllBySceneIdAndChildIdIsNull(UUID sceneId);

    Optional<SceneAudio> findBySceneIdAndSlotAndChildIdIsNull(UUID sceneId, SceneAudioSlot slot);
}
