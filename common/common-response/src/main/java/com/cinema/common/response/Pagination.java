package com.cinema.common.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Pagination {

    private final int page;

    private final int size;

    private final long total;

}
