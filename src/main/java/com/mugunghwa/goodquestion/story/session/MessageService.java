package com.mugunghwa.goodquestion.story.session;

import com.mugunghwa.goodquestion.global.vocab.CharacterEmotion;
import com.mugunghwa.goodquestion.story.session.dto.MessageResponse;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;

    public List<MessageResponse> getMessages(UUID sessionId, UUID sceneId) {
        List<Message> messages = (sceneId == null)
                ? messageRepository.findAllBySessionIdOrderByTurnOrderAsc(sessionId)
                : messageRepository.findAllBySessionIdAndSceneIdOrderByTurnOrderAsc(sessionId, sceneId);
        return messages.stream().map(MessageResponse::from).toList();
    }

    /** turn_order는 세션 전체 기준 마지막 + 1 */
    @Transactional
    public Message append(StorySession session, StoryScene scene, SpeakerType speakerType,
                          String text, String sttRawText, CharacterEmotion emotion) {
        return messageRepository.save(Message.builder()
                .session(session).scene(scene).speakerType(speakerType)
                .turnOrder(nextTurnOrder(session)).text(text).sttRawText(sttRawText)
                .characterEmotion(emotion).build());
    }

    /**
     * 아이 발화 저장. STT 부가 정보까지 함께 남긴다.
     *
     * <p>sttLowConfidence 판정은 서버 몫이지만 기준값이 아직 미정이라 지금은 항상 false다 -
     * 원값(sttConfidence)만 남겨 두면 기준이 정해진 뒤 소급해서 채울 수 있다.
     */
    @Transactional
    public Message appendChild(StorySession session, StoryScene scene, String text,
                               String sttRawText, BigDecimal sttConfidence, short sttRetryCount) {
        return messageRepository.save(Message.builder()
                .session(session).scene(scene).speakerType(SpeakerType.CHILD)
                .turnOrder(nextTurnOrder(session)).text(text).sttRawText(sttRawText)
                .sttConfidence(sttConfidence).sttLowConfidence(false).sttRetryCount(sttRetryCount)
                .build());
    }

    /** 직전 캐릭터 대사 원문. 분석 LLM이 "무엇에 대한 대답인지" 알아야 한다. 없으면 null. */
    public String lastCharacterText(UUID sessionId) {
        return messageRepository
                .findFirstBySessionIdAndSpeakerTypeOrderByTurnOrderDesc(sessionId, SpeakerType.CHARACTER)
                .map(Message::getText)
                .orElse(null);
    }

    private int nextTurnOrder(StorySession session) {
        return messageRepository.findFirstBySessionIdOrderByTurnOrderDesc(session.getId())
                .map(m -> m.getTurnOrder() + 1).orElse(1);
    }
}
