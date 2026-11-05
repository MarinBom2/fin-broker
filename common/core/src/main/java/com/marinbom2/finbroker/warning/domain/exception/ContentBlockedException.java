package com.marinbom2.finbroker.warning.domain.exception;

import com.marinbom2.finbroker.warning.domain.Warning;
import java.util.List;
import lombok.Getter;

/**
 * 저장할 수 없는 내용(BLOCK 신호)이 있다. API 에서는 400 CONTENT_BLOCKED 와 함께 warnings 를 내보낸다 (GlobalExceptionHandler).
 * 화면이 어느 부분을 지워야 하는지 보여 줄 수 있게 경고 목록을 필드로 갖는다. 근거 조각은 이미 가려져 있다.
 */
@Getter
public class ContentBlockedException extends RuntimeException {

    private final List<Warning> warnings;

    public ContentBlockedException(List<Warning> warnings) {
        super("content blocked: " + warnings.stream().map(w -> w.type().name()).distinct().toList());
        this.warnings = List.copyOf(warnings);
    }
}
