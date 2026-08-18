package com.mugunghwa.goodquestion.story.freetalk;

import com.mugunghwa.goodquestion.ai.freetalk.FreeTalkLlmClient;
import com.mugunghwa.goodquestion.ai.freetalk.FreeTalkPromptBuilder;
import com.mugunghwa.goodquestion.global.error.BusinessException;
import com.mugunghwa.goodquestion.global.error.ErrorCode;
import com.mugunghwa.goodquestion.global.vocab.CharacterEmotion;
import com.mugunghwa.goodquestion.story.content.Story;
import com.mugunghwa.goodquestion.story.content.StoryCharacter;
import com.mugunghwa.goodquestion.story.content.StoryCharacterRepository;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkCharacterResponse;
import com.mugunghwa.goodquestion.story.freetalk.dto.FreeTalkStartRequest;
import com.mugunghwa.goodquestion.story.session.SessionStatus;
import com.mugunghwa.goodquestion.story.session.StorySessionRepository;
import com.mugunghwa.goodquestion.user.child.Child;
import com.mugunghwa.goodquestion.user.child.ChildService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 자유 대화의 트랜잭션 경계. {@link FreeTalkService}가 트랜잭션 밖에서 부른다.
 *
 * <p>DB를 만지는 구간만 짧게 감싸고, 구간 사이(LLM·TTS 왕복)는 엔티티가 아니라 값으로
 * 건넌다. 학습 턴 처리에서 겪은 커넥션 점유 사고와 같은 구조다
 * (docs/트러블슈팅_턴_처리_커넥션_점유.md).
 *
 * <p><b>단, 학습 턴 처리와 한 가지가 다르다 - 아이 발화를 LLM 호출 전에 커밋하지 않는다.</b>
 * 저쪽은 분석 결과가 발화에 딸린 자산이라 먼저 남길 이유가 있지만, 여기서는 대사 생성이
 * 실패하면 "아이 말만 남고 답이 없는 턴"이 그대로 굳는다. 턴이 하나 소모된 채 아이는
 * 아무 대답도 못 듣고, 정직한 재시도는 같은 말을 두 번 남긴다. 그래서 준비(읽기)와
 * 저장(쓰기)만 트랜잭션으로 두고 그 사이에서 대사를 만든다.
 */
@Component
@RequiredArgsConstructor
public class FreeTalkTransactions {

    /**
     * 아이가 말할 수 있는 횟수. 비용·안전·과몰입 방지의 상한이고, 남은 횟수는 화면에
     * 내리지 않는다 - 세면서 하는 대화가 되면 관계가 아니라 과제가 된다.
     */
    static final int MAX_TURNS = 10;

    private final FreeTalkRepository freeTalkRepository;
    private final FreeTalkMessageRepository messageRepository;
    private final StoryCharacterRepository characterRepository;
    private final StorySessionRepository sessionRepository;
    private final ChildService childService;

    /** 대화 가능한 인물 목록. 완주하지 않은 이야기는 목록 자체를 주지 않는다(스포일러 방지). */
    @Transactional(readOnly = true)
    public List<FreeTalkCharacterResponse> characters(UUID parentId, UUID childId, UUID storyId) {
        childService.getOwnedChild(parentId, childId);
        requireCompleted(childId, storyId);

        Map<UUID, OffsetDateTime> lastTalked = freeTalkRepository
                .findLastTalkedAt(childId, storyId).stream()
                .collect(Collectors.toMap(LastTalk::characterId, LastTalk::lastTalkedAt));

        return characterRepository.findAllByStoryIdOrderByCreatedAtAscCharacterKeyAsc(storyId).stream()
                .map(character -> toResponse(character, lastTalked.get(character.getId())))
                .toList();
    }

    /**
     * 자격만 확인한다. <b>대화는 아직 만들지 않는다</b> - 첫 인사 생성이 실패하면 아이가
     * 다시 들어올 방법이 없는 빈 대화만 쌓이고, "마지막으로 이야기한 때"가 하지도 않은
     * 대화를 가리키게 된다.
     */
    @Transactional(readOnly = true)
    public FreeTalkContext prepareStart(UUID parentId, UUID childId, FreeTalkStartRequest request) {
        childService.getOwnedChild(parentId, childId);
        requireCompleted(childId, request.storyId());

        StoryCharacter character = requireCharacterOf(request.storyId(), request.characterId());
        return new FreeTalkContext(null, toResponse(character, null),
                llmInput(character, character.getStory(), List.of(), null,
                        FreeTalkPromptBuilder.STAGE_OPENING),
                0, false);
    }

    /** 첫 인사가 만들어진 뒤에야 대화를 만든다. 자격은 여기서 한 번 더 본다. */
    @Transactional
    public UUID openWithGreeting(UUID parentId, UUID childId, FreeTalkStartRequest request,
                                 FreeTalkLine greeting) {
        Child child = childService.getOwnedChild(parentId, childId);
        requireCompleted(childId, request.storyId());
        StoryCharacter character = requireCharacterOf(request.storyId(), request.characterId());

        FreeTalk talk = freeTalkRepository.save(FreeTalk.builder()
                .child(child).story(character.getStory()).character(character).build());
        append(talk, FreeTalkRole.CHARACTER, greeting.text(), greeting.emotion());
        return talk.getId();
    }

    /**
     * 다음 대사에 필요한 것을 꺼내 온다. 아이의 말은 아직 남기지 않는다 -
     * 대사와 함께 한 트랜잭션에서 남긴다.
     */
    @Transactional(readOnly = true)
    public FreeTalkContext prepareTurn(UUID parentId, UUID freeTalkId, String childText) {
        FreeTalk talk = ownedTalk(parentId, freeTalkId);
        if (talk.isEnded()) {
            throw new BusinessException(ErrorCode.FREE_TALK_ENDED);
        }

        int turnCount = talk.getTurnCount() + 1;
        boolean lastTurn = turnCount >= MAX_TURNS;
        return new FreeTalkContext(freeTalkId, null,
                llmInput(talk.getCharacter(), talk.getStory(), history(freeTalkId), childText,
                        lastTurn ? FreeTalkPromptBuilder.STAGE_CLOSING
                                : FreeTalkPromptBuilder.STAGE_TALK),
                turnCount, lastTurn);
    }

    /**
     * 아이의 말과 캐릭터의 대사를 <b>함께</b> 남긴다. 턴 수 증가와 종료도 같은 트랜잭션이다.
     *
     * <p>턴 수는 조건부로 올린다 - 준비 때 읽은 값이 그대로여야 통과한다. 겹친 요청은
     * 진 쪽이 0행을 받아 아무것도 남기지 않고 409로 돌아간다. 이것이 없으면 LLM 왕복
     * 사이에 끼어든 요청 둘 다 성공해 턴이 두 번 오르고 상한이 샌다.
     */
    @Transactional
    public void commitTurn(FreeTalkContext context, FreeTalkLine reply) {
        UUID freeTalkId = context.freeTalkId();
        short expected = context.expectedTurnCount();
        short next = (short) context.turnCount();

        int updated = context.lastTurn()
                ? freeTalkRepository.advanceTurnAndEnd(freeTalkId, expected, next,
                        OffsetDateTime.now())
                : freeTalkRepository.advanceTurn(freeTalkId, expected, next);
        if (updated == 0) {
            throw lostTheTurn(freeTalkId);
        }

        FreeTalk talk = freeTalkRepository.getReferenceById(freeTalkId);
        append(talk, FreeTalkRole.CHILD, context.llmInput().childUtterance(), null);
        append(talk, FreeTalkRole.CHARACTER, reply.text(), reply.emotion());
    }

    /** 아이가 먼저 그만둘 때. 이미 닫힌 대화면 남아 있는 마지막 대사를 그대로 들고 나온다. */
    @Transactional(readOnly = true)
    public FreeTalkEnding prepareEnd(UUID parentId, UUID freeTalkId) {
        FreeTalk talk = ownedTalk(parentId, freeTalkId);
        String characterName = talk.getCharacter().getName();

        if (talk.isEnded()) {
            return new FreeTalkEnding(characterName, null, lastCharacterLine(freeTalkId));
        }
        return new FreeTalkEnding(characterName,
                llmInput(talk.getCharacter(), talk.getStory(), history(freeTalkId), null,
                        FreeTalkPromptBuilder.STAGE_CLOSING),
                null);
    }

    /**
     * 대화를 닫고 작별 대사를 남긴다.
     *
     * <p>닫기도 조건부다. 그사이 마지막 턴이나 또 다른 "그만하기"가 먼저 닫았다면 이 요청은
     * 자기 대사를 버리고 <b>이미 남은 대사</b>를 돌려준다 - 작별 인사가 두 줄 남는 편보다
     * 한 번 만든 대사를 버리는 편이 낫다.
     */
    @Transactional
    public FreeTalkLine commitEnd(UUID freeTalkId, FreeTalkLine closing) {
        if (freeTalkRepository.endIfOpen(freeTalkId, OffsetDateTime.now()) == 0) {
            return lastCharacterLine(freeTalkId);
        }
        append(freeTalkRepository.getReferenceById(freeTalkId), FreeTalkRole.CHARACTER,
                closing.text(), closing.emotion());
        return closing;
    }

    // ----- 내부 -----

    /**
     * 완주 판정. 근거는 이 아이가 이 이야기로 만든 COMPLETED 세션이다 - 완주 횟수표
     * (child_story_play_counts)는 보상 지급이 걸린 표라 보상 규칙이 바뀌면 함께 흔들린다.
     */
    private void requireCompleted(UUID childId, UUID storyId) {
        if (!sessionRepository.existsByChildIdAndStoryIdAndStatus(
                childId, storyId, SessionStatus.COMPLETED)) {
            throw new BusinessException(ErrorCode.STORY_NOT_COMPLETED);
        }
    }

    /** 다른 이야기의 인물을 붙이면 그 인물이 모르는 세계를 아는 척하게 된다. */
    private StoryCharacter requireCharacterOf(UUID storyId, UUID characterId) {
        StoryCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "인물을 찾을 수 없습니다."));
        if (!character.getStory().getId().equals(storyId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "이 이야기의 인물이 아닙니다.");
        }
        return character;
    }

    private FreeTalk ownedTalk(UUID parentId, UUID freeTalkId) {
        FreeTalk talk = freeTalkRepository.findById(freeTalkId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "대화를 찾을 수 없습니다."));
        if (!talk.isOwnedBy(parentId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return talk;
    }

    /**
     * 조건부 갱신에서 진 요청의 사유를 가른다. 그사이 대화가 닫혔으면 끝난 것이고,
     * 아니면 다른 발화가 먼저 그 턴을 가져간 것이다 - 앞은 재시도해도 소용없고 뒤는 있다.
     */
    private BusinessException lostTheTurn(UUID freeTalkId) {
        boolean ended = freeTalkRepository.findById(freeTalkId)
                .map(FreeTalk::isEnded)
                .orElse(false);
        return new BusinessException(ended ? ErrorCode.FREE_TALK_ENDED : ErrorCode.CONCURRENT_TURN);
    }

    private void append(FreeTalk talk, FreeTalkRole role, String text, CharacterEmotion emotion) {
        messageRepository.save(FreeTalkMessage.builder()
                .freeTalk(talk)
                .role(role)
                .text(text)
                .emotion(emotion)
                .turnOrder(messageRepository.nextTurnOrder(talk.getId()))
                .build());
    }

    private FreeTalkLine lastCharacterLine(UUID freeTalkId) {
        return messageRepository
                .findFirstByFreeTalkIdAndRoleOrderByTurnOrderDesc(freeTalkId, FreeTalkRole.CHARACTER)
                .map(message -> new FreeTalkLine(message.getText(), message.getEmotion()))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "대화를 찾을 수 없습니다."));
    }

    private List<FreeTalkLlmClient.FreeTalkTurn> history(UUID freeTalkId) {
        return messageRepository.findAllByFreeTalkIdOrderByTurnOrderAsc(freeTalkId).stream()
                .map(message -> new FreeTalkLlmClient.FreeTalkTurn(
                        message.getRole().name(), message.getText()))
                .toList();
    }

    private FreeTalkLlmClient.FreeTalkLlmInput llmInput(
            StoryCharacter character, Story story,
            List<FreeTalkLlmClient.FreeTalkTurn> history, String childUtterance, String stage) {
        return new FreeTalkLlmClient.FreeTalkLlmInput(
                character.getName(), character.getPersonality(),
                story.getTitle(), story.getSummary(),
                history, childUtterance, stage);
    }

    /**
     * 표정 이미지는 클라이언트 자산이라 서버가 경로를 만들지 않는다. characterKey만 내리면
     * 클라이언트가 기존 규칙({characterKey}_{expression}.png)으로 조립한다.
     */
    private FreeTalkCharacterResponse toResponse(StoryCharacter character,
                                                 OffsetDateTime lastTalkedAt) {
        return new FreeTalkCharacterResponse(character.getId(), character.getName(),
                character.getCharacterKey(), null, lastTalkedAt);
    }
}
