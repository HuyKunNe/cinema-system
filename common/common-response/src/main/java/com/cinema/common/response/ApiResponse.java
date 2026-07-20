package com.cinema.common.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class ApiResponse<T> {

    private final boolean success;

    private final String code;

    private final String message;

    private final T data;

    private final OffsetDateTime timestamp;

    private final String traceId;

    public static <T> ApiResponse<T> success(T data, String traceId) {

        return ApiResponse.<T>builder()
                .success(true)
                .code(ResponseCode.SUCCESS.getCode())
                .message(ResponseCode.SUCCESS.getMessage())
                .data(data)
                .timestamp(OffsetDateTime.now())
                .traceId(traceId)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {

        return success(data, null);

    }

}
