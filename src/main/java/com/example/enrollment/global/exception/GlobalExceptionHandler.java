package com.example.enrollment.global.exception;

import com.example.enrollment.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // @Valid 검증 실패 (필수값 누락 등)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .get(0)
                .getDefaultMessage();
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    //비즈니스 예외 (상태 전이 오류, 정원 초과 등)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getStatus()).body(ApiResponse.fail(e.getMessage()));
    }

    // 엔티티 안에서 던지는 IllegalStateException
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()));
    }

    // 잘못된 Enum 값 또는 타입 불일치
    // ex: ?status=INVALID_STATUS 처럼 존재하지 않는 Enum 값 전달 시
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException e) {
        String message = String.format("'%s'은(는) 올바르지 않은 값입니다.", e.getValue());
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    // JSON 파싱 실패
    // ex: 숫자 필드에 문자열 전달, JSON 형식 오류 등
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(
            HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(ApiResponse.fail("요청 형식이 올바르지 않습니다."));
    }

    // 필수 쿼리 파라미터 누락
    // ex: ?studentId= 없이 GET /api/enrollments 호출 시
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException e) {
        String message = String.format("'%s' 파라미터가 필요합니다.", e.getParameterName());
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    // 존재하지 않는 URL 요청
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail("요청한 리소스를 찾을 수 없습니다."));
    }

    // 그 외 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("서버 오류가 발생했습니다."));
    }
}
