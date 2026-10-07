package com.example.delivery.global.domain.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반을 나타내는 최상위 예외. ErrorCode로 응답 상태 코드와 메시지를 결정한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
