package com.cinema.user.exception;

import com.cinema.common.exception.code.ErrorCategory;
import com.cinema.common.exception.code.ErrorCode;

public final class UserErrorCode implements ErrorCode {

    public static final UserErrorCode USER_NOT_FOUND = new UserErrorCode(
            ErrorCategory.RESOURCE,
            "USER_NOT_FOUND",
            "User not found");

    public static final UserErrorCode ROLE_NOT_FOUND = new UserErrorCode(
            ErrorCategory.RESOURCE,
            "USER_ROLE_NOT_FOUND",
            "Role not found");

    public static final UserErrorCode PERMISSION_NOT_FOUND = new UserErrorCode(
            ErrorCategory.RESOURCE,
            "USER_PERMISSION_NOT_FOUND",
            "Permission not found");

    public static final UserErrorCode USER_ROLE_ALREADY_ASSIGNED = new UserErrorCode(
            ErrorCategory.BUSINESS,
            "USER_ROLE_ALREADY_ASSIGNED",
            "Role is already assigned to user");

    public static final UserErrorCode USER_ROLE_NOT_ASSIGNED = new UserErrorCode(
            ErrorCategory.BUSINESS,
            "USER_ROLE_NOT_ASSIGNED",
            "Role is not assigned to user");

    public static final UserErrorCode ROLE_PERMISSION_ALREADY_ASSIGNED = new UserErrorCode(
            ErrorCategory.BUSINESS,
            "USER_ROLE_PERMISSION_ALREADY_ASSIGNED",
            "Permission is already assigned to role");

    public static final UserErrorCode ROLE_PERMISSION_NOT_ASSIGNED = new UserErrorCode(
            ErrorCategory.BUSINESS,
            "USER_ROLE_PERMISSION_NOT_ASSIGNED",
            "Permission is not assigned to role");

    public static final UserErrorCode SERVICE_ROLE_NOT_ASSIGNABLE_TO_USER = new UserErrorCode(
            ErrorCategory.BUSINESS,
            "USER_SERVICE_ROLE_NOT_ASSIGNABLE",
            "Service role cannot be assigned to a user account");

    public static final UserErrorCode SERVICE_ROLE_PERMISSION_NOT_ALLOWED = new UserErrorCode(
            ErrorCategory.BUSINESS,
            "USER_SERVICE_ROLE_PERMISSION_NOT_ALLOWED",
            "Service permissions must be configured per service client");

    public static final UserErrorCode USER_CREDENTIAL_NOT_FOUND = new UserErrorCode(
            ErrorCategory.RESOURCE,
            "USER_CREDENTIAL_NOT_FOUND",
            "User credential not found");

    public static final UserErrorCode USER_CREDENTIAL_ALREADY_EXISTS = new UserErrorCode(
            ErrorCategory.BUSINESS,
            "USER_CREDENTIAL_ALREADY_EXISTS",
            "User credential already exists");

    public static final UserErrorCode INVALID_CREDENTIALS = new UserErrorCode(
            ErrorCategory.SECURITY,
            "USER_INVALID_CREDENTIALS",
            "Invalid credentials");

    public static final UserErrorCode PASSWORD_REQUIRED = new UserErrorCode(
            ErrorCategory.VALIDATION,
            "USER_PASSWORD_REQUIRED",
            "Password is required");

    public static final UserErrorCode PASSWORD_TOO_SHORT = new UserErrorCode(
            ErrorCategory.VALIDATION,
            "USER_PASSWORD_TOO_SHORT",
            "Password must contain at least 12 characters");

    public static final UserErrorCode PASSWORD_TOO_LONG = new UserErrorCode(
            ErrorCategory.VALIDATION,
            "USER_PASSWORD_TOO_LONG",
            "Password exceeds the supported length");

    public static final UserErrorCode PASSWORD_MUST_DIFFER = new UserErrorCode(
            ErrorCategory.VALIDATION,
            "USER_PASSWORD_MUST_DIFFER",
            "New password must differ from the current password");

    private final ErrorCategory category;
    private final String code;
    private final String message;

    private UserErrorCode(
            ErrorCategory category,
            String code,
            String message) {

        this.category = category;
        this.code = code;
        this.message = message;
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
