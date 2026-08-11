package com.mugunghwa.goodquestion.story.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findAllBySessionIdOrderByTurnOrderAsc(UUID sessionId);

    List<Message> findAllBySessionIdAndSceneIdOrderByTurnOrderAsc(UUID sessionId, UUID sceneId);

    Optional<Message> findFirstBySessionIdOrderByTurnOrderDesc(UUID sessionId);

    /** 직전 캐릭터 발화 — 이어하기 복원 시 마지막 대사 표시용 */
    Optional<Message> findFirstBySessionIdAndSpeakerTypeOrderByTurnOrderDesc(
            UUID sessionId, SpeakerType speakerType);

    /** 장면 첫 대사 멱등 판정 — 이 장면에 이미 캐릭터 발화가 있는지 */
    Optional<Message> findFirstBySessionIdAndSceneIdAndSpeakerTypeOrderByTurnOrderAsc(
            UUID sessionId, UUID sceneId, SpeakerType speakerType);
}
