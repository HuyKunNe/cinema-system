package com.cinema.common.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Builder
public class ErrorResponse {

    private final boolean success;

    private final String code;

    private final String message;

    private final Map<String, Object> errors;

    private final OffsetDateTime timestamp;

    private final String traceId;

    public static ErrorResponse of(
            String code,
            String message,
            String traceId) {

        return ErrorResponse.builder()
                .success(false)
                .code(code)
                .message(message)
                .timestamp(OffsetDateTime.now())
                .traceId(traceId)
                .build();

    }

}
