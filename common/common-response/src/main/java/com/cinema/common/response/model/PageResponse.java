package com.cinema.common.response.model;

import java.util.List;

public record PageResponse<T>(

        List<T> content,

        PageInfo page

) {
}
