package com.mes.catalogue.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** A page of results plus the pagination metadata the UI needs. */
public record PagedResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    /** Builds a PagedResponse from a Spring Data {@link Page} of mapped content. */
    public static <T> PagedResponse<T> of(Page<?> page, List<T> content) {
        return new PagedResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
