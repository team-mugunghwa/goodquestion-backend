package com.mugunghwa.goodquestion.story.freetalk;

import com.mugunghwa.goodquestion.story.content.Story;
import com.mugunghwa.goodquestion.story.content.StoryCharacter;
import com.mugunghwa.goodquestion.user.child.Child;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 이야기를 완주한 뒤 등장인물과 이어서 하는 자유 대화 한 판.
 *
 * <p>학습 세션(story_sessions)과 일부러 갈라 두었다. 요소 판정도 유도도 별가루도 없고
 * 리포트에도 반영하지 않는다 - 같은 표에 얹었다면 리포트와 보상이 읽는 모든 조회에
 * "자유 대화는 빼고"라는 조건이 붙었을 것이고, 한 군데만 빠뜨려도 학습 지표가 조용히
 * 오염된다.
 *
 * <p>턴 상한에 닿으면 캐릭터가 자연스럽게 인사하고 대화가 닫힌다. 남은 턴은 화면에
 * 내리지 않는다 - 세다가 끝나는 대화가 아니라 그냥 끝나는 대화여야 한다.
 *
 * <p><b>turnCount와 endedAt을 여기서 고치는 메서드는 두지 않는다.</b> 두 값은
 * {@link FreeTalkRepository}의 조건부 갱신으로만 움직인다 - 준비와 저장 사이에 LLM
 * 왕복이 있어, 읽어서 고치는 방식으로는 겹친 요청 둘 다 성공해 턴 상한이 샌다.
 */
@Entity
@Table(name = "free_talks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FreeTalk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_id", nullable = false)
    private Child child;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private Story story;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "character_id", nullable = false)
    private StoryCharacter character;

    /** 아이가 말한 횟수. 캐릭터 대사는 세지 않는다. */
    @Column(name = "turn_count", nullable = false)
    private short turnCount;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public FreeTalk(Child child, Story story, StoryCharacter character) {
        this.child = child;
        this.story = story;
        this.character = character;
    }

    public boolean isEnded() {
        return endedAt != null;
    }

    public boolean isOwnedBy(UUID parentId) {
        return child.isOwnedBy(parentId);
    }
}
