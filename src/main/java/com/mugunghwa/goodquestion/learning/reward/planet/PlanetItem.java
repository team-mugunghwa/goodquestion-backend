package com.mugunghwa.goodquestion.learning.reward.planet;

import com.mugunghwa.goodquestion.learning.reward.shop.ChildItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 격자 배치 (보상-16~17).
 *
 * <p>좌표는 프론트(planet/)와 같은 축좌표(q, r)를 쓴다. 원점 기준이라 음수가 나올 수 있어
 * 하한 검증을 두지 않는다. 판 크기·모양은 클라이언트 카탈로그가 단일 소스라 서버에 두지 않는다.
 *
 * <p>한 칸에 하나(planet, q, r 유니크)와 보유 아이템 하나는 한 곳에만(childItem 유니크)을
 * DB 제약으로 보장한다 — 겹침을 코드로 막지 않는 이유다(보상-02).
 * 치우기는 이 행을 지우는 것이고 ChildItem은 남는다 = 보관함 복귀.
 *
 * <p>주의: 발판이 2x2인 아이템은 앵커 칸만 저장하므로 나머지 칸의 겹침은 이 제약이 막지 못한다.
 * TODO: 카탈로그 발판 정의를 받아 애플리케이션에서 점유 칸을 계산해 검증한다.
 */
@Entity
@Table(name = "planet_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"planet_id", "placed_q", "placed_r"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanetItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planet_id", nullable = false)
    private Planet planet;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_item_id", nullable = false, unique = true)
    private ChildItem childItem;

    @Column(name = "placed_q", nullable = false)
    private short placedQ;

    @Column(name = "placed_r", nullable = false)
    private short placedR;

    @Column(name = "placed_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime placedAt;

    @Builder
    public PlanetItem(Planet planet, ChildItem childItem, short placedQ, short placedR) {
        this.planet = planet;
        this.childItem = childItem;
        this.placedQ = placedQ;
        this.placedR = placedR;
    }

    /** 옮기기 — 같은 행성 안에서 좌표만 바꾼다. */
    public void moveTo(short placedQ, short placedR) {
        this.placedQ = placedQ;
        this.placedR = placedR;
    }
}
