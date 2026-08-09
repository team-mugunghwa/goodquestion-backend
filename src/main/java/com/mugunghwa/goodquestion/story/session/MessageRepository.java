package com.mugunghwa.goodquestion.story.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findAllBySessionIdOrderByTurnOrderAsc(UUID sessionId);

    List<Message> findAllBySessionIdAndSceneIdOrderByTurnOrderAsc(UUID sessionId, UUID sceneId);

    Optional<Message> findFirstBySessionIdOrderByTurnOrderDesc(UUID sessionId);

    /** 직전 캐릭터 발화 — 분석 LLM 입력(previousCharacterMessage)용 */
    Optional<Message> findFirstBySessionIdAndSpeakerTypeOrderByTurnOrderDesc(
            UUID sessionId, SpeakerType speakerType);
}
