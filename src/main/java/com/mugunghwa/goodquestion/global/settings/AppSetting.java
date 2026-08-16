package com.mugunghwa.goodquestion.global.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 재배포 없이 바꾸는 런타임 설정 한 줄.
 *
 * <p>이 서버는 읽기만 한다 - 쓰는 쪽은 관리자 콘솔(admin-goodquestion-backend)이다.
 * 행이 없으면 각 소비처가 환경변수 기본값으로 동작하므로, 관리자 콘솔이 없는
 * 로컬·CI에서도 지금과 똑같이 뜬다.
 */
@Entity
@Table(name = "app_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppSetting {

    /** tts.vendor 처럼 점으로 구분한 설정 이름 */
    @Id
    @Column(length = 64)
    private String key;

    @Column(nullable = false, length = 128)
    private String value;

    @Column(name = "updated_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime updatedAt;
}
