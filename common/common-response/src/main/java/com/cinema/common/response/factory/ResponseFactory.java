package com.cinema.common.response.factory;

import com.cinema.common.response.model.ApiResponse;
import com.cinema.common.response.model.ErrorBody;

import java.time.OffsetDateTime;

public final class ResponseFactory {

    private ResponseFactory() {
    }

    public static <T> ApiResponse<T> success(T data) {

        return new ApiResponse<>(

                true,

                OffsetDateTime.now(),

                data,

                null);
    }

    public static <T> ApiResponse<T> error(ErrorBody error) {

        return new ApiResponse<>(

                false,

                OffsetDateTime.now(),

                null,

                error);
    }

}
