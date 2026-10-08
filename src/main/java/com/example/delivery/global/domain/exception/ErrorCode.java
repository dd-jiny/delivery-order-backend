package com.example.delivery.global.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 애플리케이션 전체의 에러 코드.
 * domain 계층이 Spring Web에 의존하지 않도록 HttpStatus 대신 상태 코드 숫자를 가진다.
 * 도메인별 코드는 각 기능 브랜치에서 추가한다. 상태 흐름 규칙 위반은 409로 통일한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(400, "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED(401, "인증이 필요합니다."),
    ACCESS_DENIED(403, "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(404, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다."),

    // 회원
    DUPLICATE_USERNAME(409, "이미 사용 중인 아이디입니다."),
    INVALID_CREDENTIALS(401, "아이디 또는 비밀번호가 올바르지 않습니다."),

    // 메뉴
    MENU_NOT_FOUND(404, "메뉴를 찾을 수 없습니다."),
    MENU_ACCESS_DENIED(403, "본인 메뉴만 수정·삭제할 수 있습니다."),
    MENU_SOLD_OUT(409, "품절된 메뉴는 주문할 수 없습니다."),

    // 주문
    ORDER_NOT_FOUND(404, "주문을 찾을 수 없습니다."),
    ORDER_ACCESS_DENIED(403, "본인과 관련된 주문만 처리할 수 있습니다."),
    INVALID_ORDER_STATUS(409, "현재 주문 상태에서는 할 수 없는 요청입니다."),
    CONCURRENT_MODIFICATION(409, "다른 요청이 먼저 처리되었습니다. 다시 시도해 주세요.");

    private final int status;
    private final String message;
}
