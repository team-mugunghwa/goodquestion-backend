package com.mugunghwa.goodquestion.global.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        // TODO: 로깅
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
    }
}
