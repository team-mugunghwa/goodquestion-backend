package com.mugunghwa.goodquestion.learning.reward.stardust;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 최초 로그인 별가루를 이미 지급한 계정. 계정당 1행이고, 있으면 지급이 끝난 것이다.
 *
 * <p>지갑은 아이당 1개라 "계정당 1회"를 이력 표만으로는 표현할 수 없다 — 아이가 나중에
 * 늘어나면 그 아이에게는 이력이 없어 다시 지급 대상이 된다. 계정 단위 선점 기록을
 * 따로 둬야 하는 이유다.
 *
 * <p>보상 규칙이라 learning이 들고 있다. parents에 컬럼을 더하지 않은 것도 같은 이유다 —
 * user는 보상을 모른다.
 */
@Entity
@Table(name = "first_login_bonus_grants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FirstLoginBonusGrant {

    /** 계정 자체가 키다. 별도 대리키를 두면 유니크 제약을 또 걸어야 한다. */
    @Id
    @Column(name = "parent_id", nullable = false, updatable = false)
    private UUID parentId;

    @Column(name = "granted_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime grantedAt;
}
