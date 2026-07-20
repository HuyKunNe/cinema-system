package com.cinema.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseCode {

    SUCCESS("SUCCESS", "Operation successful"),

    BAD_REQUEST("BAD_REQUEST", "Invalid request"),

    UNAUTHORIZED("UNAUTHORIZED", "Authentication required"),

    FORBIDDEN("FORBIDDEN", "Access denied"),

    NOT_FOUND("NOT_FOUND", "Resource not found"),

    INTERNAL_ERROR("INTERNAL_ERROR", "Internal server error");

    private final String code;

    private final String message;

}
