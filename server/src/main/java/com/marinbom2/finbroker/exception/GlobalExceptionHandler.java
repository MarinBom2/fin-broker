package com.marinbom2.finbroker.exception;

import com.marinbom2.finbroker.community.domain.exception.NotAuthorException;
import com.marinbom2.finbroker.company.domain.exception.IllegalCompanyStateException;
import com.marinbom2.finbroker.type.common.ErrorCode;
import com.marinbom2.finbroker.warning.domain.exception.ContentBlockedException;
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
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 컨트롤러 단계 오류를 ProblemDetail + code 로 응답한다 (md/develop-principal.md §7).
 * 예외 메시지와 스택은 응답에 넣지 않고 로그로만 남긴다. 도메인 예외가 생기면 여기에 변환 규칙을 더한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** CONTENT_BLOCKED 때 찾은 신호 목록 */
    static final String WARNINGS = "warnings";

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException e) {
        log.info("business error: {} {}", e.getErrorCode(), e.getMessage());
        return ProblemDetails.of(e.getErrorCode(), e.getMessage());
    }

    /** 예외 메시지(업체 ID·상태)는 로그로만 */
    @ExceptionHandler(IllegalCompanyStateException.class)
    public ProblemDetail handleIllegalCompanyState(IllegalCompanyStateException e) {
        log.info("company state conflict: {}", e.getMessage());
        return ProblemDetails.of(ErrorCode.COMPANY_STATE_CONFLICT);
    }

    /** 저장할 수 없는 내용: 어느 신호인지 화면이 보여 줄 수 있게 warnings 를 함께 (근거 조각은 이미 가려져 있다) */
    @ExceptionHandler(ContentBlockedException.class)
    public ProblemDetail handleContentBlocked(ContentBlockedException e) {
        log.info("content blocked: {}", e.getMessage());
        ProblemDetail problem = ProblemDetails.of(ErrorCode.CONTENT_BLOCKED);
        problem.setProperty(WARNINGS, e.getWarnings().stream()
                .map(w -> Map.of("type", w.type(), "evidence", w.evidence()))
                .toList());
        return problem;
    }

    /** 작성자가 아닌 회원의 고치기·지우기 */
    @ExceptionHandler(NotAuthorException.class)
    public ProblemDetail handleNotAuthor(NotAuthorException e) {
        log.info("not author: {}", e.getMessage());
        return ProblemDetails.of(ErrorCode.FORBIDDEN, "내가 쓴 글·답변만 고치거나 지울 수 있어요.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error("unexpected error", e);
        return ProblemDetails.of(ErrorCode.INTERNAL_ERROR);
    }

    /** 요청 파라미터 검증(@Min 등) 실패: 본문 검증과 같은 INVALID_REQUEST_DATA + errors */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetails.of(ErrorCode.INVALID_REQUEST_DATA);
        List<Map<String, String>> errors = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> Map.of(
                                "field", String.valueOf(result.getMethodParameter().getParameterName()),
                                "message", String.valueOf(error.getDefaultMessage()))))
                .toList();
        problem.setProperty(ProblemDetails.ERRORS, errors);
        return handleExceptionInternal(e, problem, headers, status, request);
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
