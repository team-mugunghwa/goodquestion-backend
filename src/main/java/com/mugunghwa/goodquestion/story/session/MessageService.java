package com.mugunghwa.goodquestion.story.session;

import com.mugunghwa.goodquestion.global.vocab.CharacterEmotion;
import com.mugunghwa.goodquestion.story.session.dto.MessageResponse;
import com.mugunghwa.goodquestion.story.content.StoryScene;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        int nextTurn = messageRepository.findFirstBySessionIdOrderByTurnOrderDesc(session.getId())
                .map(m -> m.getTurnOrder() + 1).orElse(1);
        return messageRepository.save(Message.builder()
                .session(session).scene(scene).speakerType(speakerType)
                .turnOrder(nextTurn).text(text).sttRawText(sttRawText)
                .characterEmotion(emotion).build());
    }
}
