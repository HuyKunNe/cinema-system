package com.cinema.common.response.model;

import java.time.OffsetDateTime;

public record ApiResponse<T>(

        boolean success,

        OffsetDateTime timestamp,

        T data,

        ErrorBody error

) {
}
