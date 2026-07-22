package com.cinema.common.exception.code;

public enum CommonErrorCode implements ErrorCode {

    RESOURCE_LOCKED(
            "RESOURCE_LOCKED",
            "Resource is currently locked",
            ErrorCategory.RESOURCE);

    private final String code;

    private final String message;

    private final ErrorCategory category;

    CommonErrorCode(
            String code,
            String message,
            ErrorCategory category) {

        this.code = code;
        this.message = message;
        this.category = category;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public ErrorCategory category() {
        return category;
    }

}
