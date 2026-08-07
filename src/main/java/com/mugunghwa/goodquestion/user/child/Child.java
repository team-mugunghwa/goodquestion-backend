package com.mugunghwa.goodquestion.user.child;

import com.mugunghwa.goodquestion.user.parent.Parent;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.UUID;

/** 보호자가 등록한 아이 프로필. */
@Entity
@Table(name = "children")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Child {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "birth_year", nullable = false)
    private short birthYear;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    @Builder
    public Child(Parent parent, String name, short birthYear) {
        this.parent = parent;
        this.name = name;
        this.birthYear = birthYear;
    }

    public void update(String name, Short birthYear) {
        if (name != null) this.name = name;
        if (birthYear != null) this.birthYear = birthYear;
    }

    /** 출생연도 기준 연령 (정확한 만 나이 아님) */
    public int getAge() {
        return Year.now().getValue() - birthYear;
    }

    public boolean isOwnedBy(UUID parentId) {
        return parent.getId().equals(parentId);
    }
}
