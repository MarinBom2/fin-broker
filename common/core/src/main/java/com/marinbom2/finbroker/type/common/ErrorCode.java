package com.marinbom2.finbroker.type.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * API 오류 코드. ProblemDetail 의 status·code·detail 이 여기서 나온다 (md/develop-principal.md §7).
 * description 은 화면 문구다. status 는 core 에 spring-web 이 없어 int.
 */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = Shape.OBJECT)
public enum ErrorCode implements DescriptionCode {

    /* 400 */
    BAD_REQUEST(400, "잘못된 요청입니다."),
    INVALID_REQUEST_DATA(400, "입력값을 다시 확인해 주세요."),
    CONTENT_BLOCKED(400, "글에 쓸 수 없는 내용이 있어요. 표시된 부분을 지우고 다시 올려 주세요."),
    SELF_REPORT(400, "내가 쓴 글이나 답변은 신고할 수 없어요."),

    /* 401 */
    UNAUTHORIZED(401, "로그인이 필요합니다."),
    INVALID_ACCESS_TOKEN(401, "로그인 정보가 유효하지 않습니다. 다시 로그인해 주세요."),
    LOGIN_FAILED(401, "아이디 또는 비밀번호가 일치하지 않습니다."),

    /* 403 */
    FORBIDDEN(403, "요청할 권한이 없습니다."),

    /* 404 */
    NOT_FOUND(404, "올바른 경로가 아닙니다."),
    NOT_FOUND_DATA(404, "요청한 정보를 찾을 수 없습니다."),
    NOT_FOUND_MEMBER(404, "회원 정보를 찾을 수 없습니다."),
    NOT_FOUND_COMPANY(404, "업체 정보를 찾을 수 없습니다."),
    NOT_FOUND_POST(404, "글을 찾을 수 없습니다. 삭제되었거나 숨겨진 글일 수 있습니다."),
    NOT_FOUND_ANSWER(404, "답변을 찾을 수 없습니다. 삭제되었거나 숨겨진 답변일 수 있습니다."),
    NOT_FOUND_REPORT(404, "신고를 찾을 수 없습니다."),

    /* 405 */
    METHOD_NOT_ALLOWED(405, "지원하지 않는 요청 방식입니다."),

    /* 409 */
    CONFLICT(409, "요청이 현재 상태와 충돌합니다."),
    ALREADY_EXIST(409, "이미 존재합니다."),
    COMPANY_STATE_CONFLICT(409, "지금 업체 등록 상태에서는 할 수 없는 처리입니다."),
    ALREADY_REPORTED(409, "이미 신고했어요. 운영자가 확인하고 있어요."),

    /* 413 */
    PAYLOAD_TOO_LARGE(413, "요청 크기가 너무 큽니다."),

    /* 429 */
    WAIT_REQUEST_TIMEOUT(429, "잠시 후 다시 시도해 주세요."),

    /* 500 */
    INTERNAL_ERROR(500, "서버에서 오류가 발생했습니다."),

    /* 503 */
    SERVICE_UNAVAILABLE(503, "지금은 요청을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요.");

    private final int status;
    private final String description;

    @Override
    public String getDescription() {
        return description;
    }

    /** 상태 코드만 있는 오류(Spring 이 직접 낸 404, 405 등)에 붙일 기본 코드 */
    public static ErrorCode defaultFor(int status) {
        return switch (status) {
            case 401 -> UNAUTHORIZED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 409 -> CONFLICT;
            case 413 -> PAYLOAD_TOO_LARGE;
            case 429 -> WAIT_REQUEST_TIMEOUT;
            case 503 -> SERVICE_UNAVAILABLE;
            default -> status >= 500 ? INTERNAL_ERROR : BAD_REQUEST;
        };
    }
}
