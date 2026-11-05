package com.marinbom2.finbroker.exception;

import com.marinbom2.finbroker.type.common.ErrorCode;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 컨트롤러 단계 오류를 ProblemDetail + code 로 응답한다 (md/develop-principal.md §7).
 * 예외 메시지와 스택은 응답에 넣지 않고 로그로만 남긴다. 도메인 예외가 생기면 여기에 변환 규칙을 더한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException e) {
        log.info("business error: {} {}", e.getErrorCode(), e.getMessage());
        return ProblemDetails.of(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error("unexpected error", e);
        return ProblemDetails.of(ErrorCode.INTERNAL_ERROR);
    }

    /** @Valid 검증 실패: 필드별 오류를 errors 로 함께 내보낸다 */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetails.of(ErrorCode.INVALID_REQUEST_DATA);
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> Map.of(
                        "field", fieldError.getField(),
                        "message", String.valueOf(fieldError.getDefaultMessage())))
                .toList();
        problem.setProperty(ProblemDetails.ERRORS, errors);
        return handleExceptionInternal(e, problem, headers, status, request);
    }

    /**
     * 부모 클래스가 처리하는 Spring MVC 예외 전부가 여기를 지난다.
     * 부모는 본문을 비운 채(null) 넘기고 여기서 ProblemDetail 을 만들기 때문에, 부모가 응답을 만든 뒤에 손본다.
     * code 가 없는 응답에는 상태에 맞는 기본 code 와 한글 문구를 입힌다. Spring 의 영문 detail 은 로그로만 남긴다.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception e, @Nullable Object body, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(e, body, headers, status, request);
        if (response != null
                && response.getBody() instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey(ProblemDetails.CODE))) {
            ErrorCode errorCode = ErrorCode.defaultFor(status.value());
            log.debug("spring mvc error {}: {}", status.value(), problem.getDetail());
            problem.setTitle(HttpStatus.valueOf(status.value()).getReasonPhrase());
            problem.setDetail(errorCode.getDescription());
            problem.setProperty(ProblemDetails.CODE, errorCode.getCode());
        }
        return response;
    }
}
