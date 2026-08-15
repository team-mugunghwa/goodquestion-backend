package com.mugunghwa.goodquestion.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 낙관적 락 충돌 -> 409. 한 턴 처리는 STT/분석/대사 생성으로 수 초가 걸려, 아이가 연타하면
     * 같은 세션에 두 턴이 겹친다. 500으로 나가면 클라이언트가 재시도해도 되는지 알 수 없다 -
     * 409로 내려 "앞선 요청이 끝난 뒤 다시 보내라"는 뜻을 분명히 한다.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentUpdate(OptimisticLockingFailureException e) {
        return ResponseEntity.status(ErrorCode.CONCURRENT_TURN.getStatus())
                .body(ErrorResponse.of(ErrorCode.CONCURRENT_TURN,
                        ErrorCode.CONCURRENT_TURN.getDefaultMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ErrorResponse.of(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst().map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_REQUEST", message));
    }

    /**
     * 미구현 스텁. 컨트롤러 골격만 있고 로직이 없는 엔드포인트가 호출되면 501로 명확히 알린다 —
     * 200에 빈 본문을 돌려주면 프론트가 구현된 것으로 오해한다.
     */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleNotImplemented(UnsupportedOperationException e) {
        String message = e.getMessage() != null ? e.getMessage() : ErrorCode.NOT_IMPLEMENTED.getDefaultMessage();
        return ResponseEntity.status(ErrorCode.NOT_IMPLEMENTED.getStatus())
                .body(ErrorResponse.of(ErrorCode.NOT_IMPLEMENTED, message));
    }

    /**
     * AI 벤더가 4xx/5xx를 돌려준 경우. 쿼터 소진(429)과 키 문제와 우리 요청 버그는
     * 대응이 완전히 다른데 전부 500으로 나가면 현장에서 원인을 가릴 수 없다.
     * 벤더 응답 본문은 클라이언트에 싣지 않는다 — 조직명·키 힌트가 실려 올 수 있어
     * 로그에만 남긴다.
     */
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleVendorResponse(WebClientResponseException e) {
        log.error("AI 벤더 응답 오류 status={}", e.getStatusCode());
        ErrorCode code = e.getStatusCode().value() == 429
                ? ErrorCode.AI_RATE_LIMITED : ErrorCode.AI_UPSTREAM_ERROR;
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, code.getDefaultMessage()));
    }

    /** 벤더 연결 실패·타임아웃. 재시도하면 되는 부류라 503으로 내린다. */
    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ErrorResponse> handleVendorUnreachable(WebClientRequestException e) {
        log.error("AI 벤더 연결 실패: {}", e.getMessage());
        return ResponseEntity.status(ErrorCode.AI_UNAVAILABLE.getStatus())
                .body(ErrorResponse.of(ErrorCode.AI_UNAVAILABLE,
                        ErrorCode.AI_UNAVAILABLE.getDefaultMessage()));
    }

    /** 업로드 한도 초과. 500이 아니라 413 — 프론트가 "짧게 말해요"를 띄울 수 있어야 한다. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(ErrorCode.AUDIO_TOO_LARGE.getStatus())
                .body(ErrorResponse.of(ErrorCode.AUDIO_TOO_LARGE,
                        ErrorCode.AUDIO_TOO_LARGE.getDefaultMessage()));
    }

    /** 본문 파싱 실패는 클라이언트 버그다. 500으로 뭉개지 않고 400으로 알린다. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("INVALID_REQUEST", "요청 본문을 읽을 수 없습니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        log.error("처리되지 않은 예외", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
    }
}
