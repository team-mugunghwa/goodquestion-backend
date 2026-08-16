package com.mugunghwa.goodquestion.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    CONSENT_REQUIRED(HttpStatus.CONFLICT, "유효한 아동 동의가 필요합니다."),
    SESSION_NOT_IN_PROGRESS(HttpStatus.CONFLICT, "진행 중인 세션이 아닙니다."),
    SCENE_NOT_STORY(HttpStatus.CONFLICT, "현재 장면은 스토리 장면이 아닙니다."),
    SCENE_NOT_DIALOGUE(HttpStatus.CONFLICT, "현재 장면은 대화 장면이 아닙니다."),
    REPORT_NOT_READY(HttpStatus.CONFLICT, "리포트가 아직 생성되지 않았습니다."),
    DUPLICATE_WORD(HttpStatus.CONFLICT, "이미 저장된 단어입니다."),
    INQUIRY_ALREADY_ANSWERED(HttpStatus.CONFLICT, "답변이 완료된 문의는 수정하거나 삭제할 수 없습니다."),
    STT_EMPTY_TEXT(HttpStatus.UNPROCESSABLE_ENTITY, "음성에서 텍스트를 인식하지 못했습니다."),
    INVALID_WORD(HttpStatus.UNPROCESSABLE_ENTITY, "단어장에 담기 어려운 말입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "로그인 시도가 많아 계정이 일시적으로 잠겼습니다."),
    // 관리자가 막은 계정. 잠금(ACCOUNT_LOCKED)과 구분해야 한다 - 잠금은 기다리면 풀리고
    // 정지는 문의해야 풀린다. 하나로 뭉치면 사용자가 몇 시간을 기다리다 다시 시도한다.
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "이용이 제한된 계정입니다. 고객센터로 문의해 주세요."),
    KAKAO_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "카카오 인증에 실패했습니다."),
    GOOGLE_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "구글 인증에 실패했습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),
    INVALID_PASSWORD_RESET_TOKEN(HttpStatus.BAD_REQUEST, "비밀번호 재설정 링크가 유효하지 않거나 만료되었습니다."),
    EMAIL_DELIVERY_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "이메일을 발송할 수 없습니다. 잠시 후 다시 시도해 주세요."),

    // ---- 보상(행성 꾸미기) — 엔티티 구현 후 사용 ----
    CELL_OCCUPIED(HttpStatus.CONFLICT, "이미 다른 아이템이 놓인 칸입니다."),
    ITEM_ALREADY_PLACED(HttpStatus.CONFLICT, "이미 행성에 놓인 아이템입니다."),
    ITEM_LOCKED(HttpStatus.CONFLICT, "아직 열리지 않은 아이템입니다."),
    STARDUST_INSUFFICIENT(HttpStatus.CONFLICT, "별가루가 부족합니다."),
    GRID_OUT_OF_RANGE(HttpStatus.UNPROCESSABLE_ENTITY, "격자 범위를 벗어났습니다."),

    // ---- 멱등키 ----
    REQUEST_IN_PROGRESS(HttpStatus.CONFLICT, "같은 요청을 처리하는 중입니다. 잠시 후 다시 시도해 주세요."),
    INVALID_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, "Idempotency-Key는 64자 이하여야 합니다."),

    // ---- 진행 단계 충돌 ----
    CONCURRENT_TURN(HttpStatus.CONFLICT, "앞선 발화를 처리하는 중입니다. 잠시 후 다시 시도해 주세요."),
    MAX_TURNS_EXCEEDED(HttpStatus.CONFLICT, "이 장면의 대화가 이미 끝났습니다."),
    MISSION_NOT_EXPOSED(HttpStatus.CONFLICT, "아직 노출되지 않은 미션입니다."),
    RETELLING_BEFORE_ORDER(HttpStatus.CONFLICT, "카드 순서를 맞춘 뒤에 이야기를 다시 들려줄 수 있습니다."),

    // ---- AI 벤더 연동 (08-15 감사: 전부 500으로 뭉개지던 것을 구분) ----
    AI_RATE_LIMITED(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스가 혼잡합니다. 잠시 후 다시 시도해 주세요."),
    AI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요."),
    AI_UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "AI 서비스 응답에 문제가 있습니다."),
    AUDIO_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "녹음이 너무 깁니다. 짧게 나눠 말해 주세요."),

    NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "아직 구현되지 않은 기능입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
