package com.booking.system.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * A stable, framework-agnostic pagination envelope. We deliberately don't
 * return Spring Data's Page<T> directly from controllers - its JSON shape
 * has changed across versions and leaks implementation details (pageable,
 * sort object graph) that callers shouldn't need to parse.
 */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
