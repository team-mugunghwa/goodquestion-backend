package com.mugunghwa.goodquestion.story.freetalk;

import com.mugunghwa.goodquestion.global.vocab.CharacterEmotion;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 자유 대화 한 줄. 아이 음성 원본은 저장하지 않고 텍스트만 남긴다(기존 원칙).
 *
 * <p>STT 신뢰도를 함께 남기지 않는 것도 의도다. 그 값은 리포트가 대표 발화를 고를 때
 * 쓰는데 자유 대화는 리포트에 들어가지 않으므로 남길 이유가 없다.
 */
@Entity
@Table(name = "free_talk_messages",
        uniqueConstraints = @UniqueConstraint(columnNames = {"free_talk_id", "turn_order"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FreeTalkMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "free_talk_id", nullable = false)
    private FreeTalk freeTalk;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FreeTalkRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    /** 캐릭터 발화에만 값이 있다. 화면의 표정 전환에 쓴다. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CharacterEmotion emotion;

    /**
     * 대화 안에서의 순서. 0은 캐릭터의 첫 인사다.
     *
     * <p>created_at으로 정렬하지 않는 이유는 PostgreSQL의 now()가 트랜잭션 시작 시각이라
     * 같은 트랜잭션에서 저장된 두 줄이 같은 값을 갖기 때문이다. 이 이력은 LLM 입력이라
     * 순서가 흔들리면 문맥이 통째로 어긋난다.
     */
    @Column(name = "turn_order", nullable = false)
    private short turnOrder;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public FreeTalkMessage(FreeTalk freeTalk, FreeTalkRole role, String text,
                           CharacterEmotion emotion, short turnOrder) {
        this.freeTalk = freeTalk;
        this.role = role;
        this.text = text;
        this.emotion = emotion;
        this.turnOrder = turnOrder;
    }
}
