package com.example.delivery.global.presentation;

import com.example.delivery.global.domain.exception.ErrorCode;
import java.util.List;

/**
 * 에러 응답 본문. fieldErrors는 요청 값 검증 실패일 때만 채워진다.
 */
public record ErrorResponse(
        int status,
        String code,
        String message,
        List<FieldError> fieldErrors
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getStatus(), errorCode.name(), message, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fieldErrors) {
        return new ErrorResponse(errorCode.getStatus(), errorCode.name(), errorCode.getMessage(), fieldErrors);
    }

    public record FieldError(String field, String reason) {
    }
}
