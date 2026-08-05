package com.cinema.common.security.error;

import com.cinema.common.exception.code.ErrorCategory;
import com.cinema.common.exception.code.ErrorCode;

public enum SecurityErrorCode implements ErrorCode {

    INVALID_AUDIENCE_CONFIGURATION(
            "SECURITY_INVALID_AUDIENCE_CONFIGURATION",
            "OAuth2 audience configuration is invalid",
            ErrorCategory.SYSTEM),

    INVALID_ISSUER_CONFIGURATION(
            "SECURITY_INVALID_ISSUER_CONFIGURATION",
            "OAuth2 issuer configuration is invalid",
            ErrorCategory.SYSTEM),

    INVALID_JWT_SUBJECT(
            "SECURITY_INVALID_JWT_SUBJECT",
            "JWT subject is invalid",
            ErrorCategory.SECURITY),

    AUTHENTICATION_REQUIRED(
            "SECURITY_AUTHENTICATION_REQUIRED",
            "Authentication is required",
            ErrorCategory.SECURITY),

    INVALID_JWK_SET_CONFIGURATION(
            "SECURITY_INVALID_JWK_SET_CONFIGURATION",
            "OAuth2 JWK Set configuration is invalid",
            ErrorCategory.SYSTEM),

    UNSUPPORTED_AUTHENTICATED_PRINCIPAL(
            "SECURITY_UNSUPPORTED_PRINCIPAL",
            "Authenticated principal type is unsupported",
            ErrorCategory.SECURITY),

    ACCESS_DENIED(
            "SECURITY_ACCESS_DENIED",
            "Access is denied",
            ErrorCategory.SECURITY);

    private final String code;

    private final String message;

    private final ErrorCategory category;

    SecurityErrorCode(
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
