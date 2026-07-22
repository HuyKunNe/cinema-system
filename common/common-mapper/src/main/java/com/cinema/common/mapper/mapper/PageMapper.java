package com.cinema.common.mapper.mapper;

import org.springframework.data.domain.Page;

import com.cinema.common.api.mapper.PageResponseMapper;
import com.cinema.common.response.model.PageResponse;

public final class PageMapper {

    private PageMapper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static <T> PageResponse<T> toPage(Page<T> page) {
        return PageResponseMapper.map(page);
    }

}
