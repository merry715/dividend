package com.example.dividend.exception;

import com.example.dividend.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 전역 유효성 검사 실패 처리.
 * 각 Controller의 @ExceptionHandler(MethodArgumentNotValidException.class) 를 대체한다.
 * — StockController / AuthController 의 로컬 핸들러가 먼저 호출되므로 충돌 없음.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message, "INVALID_INPUT"));
    }
}
