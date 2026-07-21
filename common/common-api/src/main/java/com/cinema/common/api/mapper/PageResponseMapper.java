package com.cinema.common.api.mapper;

import org.springframework.data.domain.Page;

import com.cinema.common.response.model.PageInfo;
import com.cinema.common.response.model.PageResponse;

public final class PageResponseMapper {

    private PageResponseMapper() {
    }

    public static <T> PageResponse<T> map(
            Page<T> page) {

        return new PageResponse<>(
                page.getContent(),
                new PageInfo(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.isFirst(),
                        page.isLast()));
    }
}
