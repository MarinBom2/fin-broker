package com.marinbom2.finbroker.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * 목록 응답. 페이지 번호는 1부터 (Spring Data 의 0부터를 API 밖으로 내보내지 않는다).
 * client shared/types/common.ts 의 PageResponse 와 1:1.
 */
@Schema(description = "목록 한 페이지")
public record PageResponse<T>(
        @Schema(description = "이 페이지의 항목") List<T> content,
        @Schema(description = "페이지 번호 (1부터)", example = "1") int pageNumber,
        @Schema(description = "페이지 크기", example = "20") int pageSize,
        @Schema(description = "전체 항목 수", example = "42") long totalSize,
        @Schema(description = "전체 페이지 수", example = "3") int totalPages,
        @Schema(description = "다음 페이지가 있는지") boolean hasNext
) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext());
    }
}
