package com.marinbom2.finbroker.community.domain.exception;

/** 작성자가 아닌 회원이 글·답변을 고치거나 지우려 했다. API 에서는 403 FORBIDDEN 으로 바꾼다 (GlobalExceptionHandler) */
public class NotAuthorException extends RuntimeException {

    public NotAuthorException(String target, Long targetId, Long memberId) {
        super(target + " " + targetId + " is not written by member " + memberId);
    }
}
