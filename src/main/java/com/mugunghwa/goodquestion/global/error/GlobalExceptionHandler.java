package com.mugunghwa.goodquestion.global.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        log.error("처리되지 않은 예외", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
    }
}
