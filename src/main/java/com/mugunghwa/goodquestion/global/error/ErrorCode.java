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
    KAKAO_AUTH_FAILED(HttpStatus.UNAUTHORIZED, "카카오 인증에 실패했습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String defaultMessage;
}
