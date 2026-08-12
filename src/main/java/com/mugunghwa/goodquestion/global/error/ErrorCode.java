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
    STT_EMPTY_TEXT(HttpStatus.UNPROCESSABLE_ENTITY, "음성에서 텍스트를 인식하지 못했습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "로그인 시도가 많아 계정이 일시적으로 잠겼습니다."),
    KAKAO_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "카카오 인증에 실패했습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),

    // ---- 보상(행성 꾸미기) — 엔티티 구현 후 사용 ----
    CELL_OCCUPIED(HttpStatus.CONFLICT, "이미 다른 아이템이 놓인 칸입니다."),
    ITEM_ALREADY_PLACED(HttpStatus.CONFLICT, "이미 행성에 놓인 아이템입니다."),
    ITEM_LOCKED(HttpStatus.CONFLICT, "아직 열리지 않은 아이템입니다."),
    STARDUST_INSUFFICIENT(HttpStatus.CONFLICT, "별가루가 부족합니다."),
    GRID_OUT_OF_RANGE(HttpStatus.UNPROCESSABLE_ENTITY, "격자 범위를 벗어났습니다."),

    // ---- 진행 단계 충돌 ----
    CONCURRENT_TURN(HttpStatus.CONFLICT, "앞선 발화를 처리하는 중입니다. 잠시 후 다시 시도해 주세요."),
    MAX_TURNS_EXCEEDED(HttpStatus.CONFLICT, "이 장면의 대화가 이미 끝났습니다."),
    MISSION_NOT_EXPOSED(HttpStatus.CONFLICT, "아직 노출되지 않은 미션입니다."),
    MISSION_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 제출한 미션입니다."),
    RETELLING_BEFORE_ORDER(HttpStatus.CONFLICT, "카드 순서를 맞춘 뒤에 이야기를 다시 들려줄 수 있습니다."),

    NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "아직 구현되지 않은 기능입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
