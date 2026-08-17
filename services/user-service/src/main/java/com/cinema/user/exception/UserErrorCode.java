package com.cinema.user.exception;

import com.cinema.common.exception.code.ErrorCategory;
import com.cinema.common.exception.code.ErrorCode;

public final class UserErrorCode implements ErrorCode {

    public static final UserErrorCode USER_NOT_FOUND =
            new UserErrorCode(ErrorCategory.RESOURCE, "USER_NOT_FOUND", "User not found");

    public static final UserErrorCode ROLE_NOT_FOUND =
            new UserErrorCode(ErrorCategory.RESOURCE, "USER_ROLE_NOT_FOUND", "Role not found");

    public static final UserErrorCode PERMISSION_NOT_FOUND =
            new UserErrorCode(
                    ErrorCategory.RESOURCE, "USER_PERMISSION_NOT_FOUND", "Permission not found");

    public static final UserErrorCode USER_ROLE_ALREADY_ASSIGNED =
            new UserErrorCode(
                    ErrorCategory.BUSINESS,
                    "USER_ROLE_ALREADY_ASSIGNED",
                    "Role is already assigned to user");

    public static final UserErrorCode USER_ROLE_NOT_ASSIGNED =
            new UserErrorCode(
                    ErrorCategory.BUSINESS,
                    "USER_ROLE_NOT_ASSIGNED",
                    "Role is not assigned to user");

    public static final UserErrorCode ROLE_PERMISSION_ALREADY_ASSIGNED =
            new UserErrorCode(
                    ErrorCategory.BUSINESS,
                    "USER_ROLE_PERMISSION_ALREADY_ASSIGNED",
                    "Permission is already assigned to role");

    public static final UserErrorCode ROLE_PERMISSION_NOT_ASSIGNED =
            new UserErrorCode(
                    ErrorCategory.BUSINESS,
                    "USER_ROLE_PERMISSION_NOT_ASSIGNED",
                    "Permission is not assigned to role");

    public static final UserErrorCode SERVICE_ROLE_NOT_ASSIGNABLE_TO_USER =
            new UserErrorCode(
                    ErrorCategory.BUSINESS,
                    "USER_SERVICE_ROLE_NOT_ASSIGNABLE",
                    "Service role cannot be assigned to a user account");

    public static final UserErrorCode SERVICE_ROLE_PERMISSION_NOT_ALLOWED =
            new UserErrorCode(
                    ErrorCategory.BUSINESS,
                    "USER_SERVICE_ROLE_PERMISSION_NOT_ALLOWED",
                    "Service permissions must be configured per service client");

    public static final UserErrorCode USER_CREDENTIAL_NOT_FOUND =
            new UserErrorCode(
                    ErrorCategory.RESOURCE,
                    "USER_CREDENTIAL_NOT_FOUND",
                    "User credential not found");

    public static final UserErrorCode USER_CREDENTIAL_ALREADY_EXISTS =
            new UserErrorCode(
                    ErrorCategory.BUSINESS,
                    "USER_CREDENTIAL_ALREADY_EXISTS",
                    "User credential already exists");

    public static final UserErrorCode INVALID_CREDENTIALS =
            new UserErrorCode(
                    ErrorCategory.SECURITY, "USER_INVALID_CREDENTIALS", "Invalid credentials");

    public static final UserErrorCode PASSWORD_REQUIRED =
            new UserErrorCode(
                    ErrorCategory.VALIDATION, "USER_PASSWORD_REQUIRED", "Password is required");

    public static final UserErrorCode PASSWORD_TOO_SHORT =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_PASSWORD_TOO_SHORT",
                    "Password must contain at least 12 characters");

    public static final UserErrorCode PASSWORD_TOO_LONG =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_PASSWORD_TOO_LONG",
                    "Password exceeds the supported length");

    public static final UserErrorCode PASSWORD_MUST_DIFFER =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_PASSWORD_MUST_DIFFER",
                    "New password must differ from the current password");

    public static final UserErrorCode ACCOUNT_STATE_TRANSITION_NOT_ALLOWED =
            new UserErrorCode(
                    ErrorCategory.BUSINESS,
                    "USER_ACCOUNT_STATE_TRANSITION_NOT_ALLOWED",
                    "Account state transition is not allowed");

    public static final UserErrorCode ACCOUNT_TIMESTAMP_REQUIRED =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_ACCOUNT_TIMESTAMP_REQUIRED",
                    "Account transition timestamp is required");

    public static final UserErrorCode EMAIL_VERIFICATION_TOKEN_HASH_INVALID =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_EMAIL_VERIFICATION_TOKEN_HASH_INVALID",
                    "Email verification token hash is invalid");

    public static final UserErrorCode EMAIL_VERIFICATION_EXPIRATION_REQUIRED =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_EMAIL_VERIFICATION_EXPIRATION_REQUIRED",
                    "Email verification expiration is required");

    public static final UserErrorCode EMAIL_VERIFICATION_TIMESTAMP_REQUIRED =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_EMAIL_VERIFICATION_TIMESTAMP_REQUIRED",
                    "Email verification timestamp is required");

    public static final UserErrorCode EMAIL_VERIFICATION_TOKEN_NOT_USABLE =
            new UserErrorCode(
                    ErrorCategory.BUSINESS,
                    "USER_EMAIL_VERIFICATION_TOKEN_NOT_USABLE",
                    "Email verification token is not usable");

    public static final UserErrorCode EMAIL_VERIFICATION_TOKEN_EXPIRED =
            new UserErrorCode(
                    ErrorCategory.BUSINESS,
                    "USER_EMAIL_VERIFICATION_TOKEN_EXPIRED",
                    "Email verification token has expired");

    public static final UserErrorCode EMAIL_VERIFICATION_TOKEN_INVALID =
            new UserErrorCode(
                    ErrorCategory.SECURITY,
                    "USER_EMAIL_VERIFICATION_TOKEN_INVALID",
                    "Email verification token is invalid");

    public static final UserErrorCode EMAIL_VERIFICATION_TOKEN_ISSUE_NOT_ALLOWED =
            new UserErrorCode(
                    ErrorCategory.BUSINESS,
                    "USER_EMAIL_VERIFICATION_TOKEN_ISSUE_NOT_ALLOWED",
                    "Email verification token cannot be issued for this account");

    public static final UserErrorCode EMAIL_VERIFICATION_TOKEN_ISSUE_FAILED =
            new UserErrorCode(
                    ErrorCategory.SYSTEM,
                    "USER_EMAIL_VERIFICATION_TOKEN_ISSUE_FAILED",
                    "Email verification token could not be issued");

    public static final UserErrorCode EMAIL_VERIFICATION_CRYPTO_FAILURE =
            new UserErrorCode(
                    ErrorCategory.SYSTEM,
                    "USER_EMAIL_VERIFICATION_CRYPTO_FAILURE",
                    "Email verification cryptography is unavailable");

    public static final UserErrorCode OAUTH2_CLIENT_ID_REQUIRED =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_OAUTH2_CLIENT_ID_REQUIRED",
                    "OAuth2 client identifier is required");

    public static final UserErrorCode OAUTH2_CLIENT_NAME_REQUIRED =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_OAUTH2_CLIENT_NAME_REQUIRED",
                    "OAuth2 client name is required");

    public static final UserErrorCode OAUTH2_CLIENT_SECRET_REQUIRED =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_OAUTH2_CLIENT_SECRET_REQUIRED",
                    "OAuth2 client secret is required");

    public static final UserErrorCode OAUTH2_CLIENT_REDIRECT_URI_INVALID =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_OAUTH2_CLIENT_REDIRECT_URI_INVALID",
                    "OAuth2 client redirect URI is invalid");

    public static final UserErrorCode OAUTH2_CLIENT_SCOPE_INVALID =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_OAUTH2_CLIENT_SCOPE_INVALID",
                    "OAuth2 client scope is invalid");

    public static final UserErrorCode OAUTH2_CLIENT_ALREADY_EXISTS =
            new UserErrorCode(
                    ErrorCategory.BUSINESS,
                    "USER_OAUTH2_CLIENT_ALREADY_EXISTS",
                    "OAuth2 client already exists");

    public static final UserErrorCode OAUTH2_REFRESH_TOKEN_HISTORY_INVALID =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_OAUTH2_REFRESH_TOKEN_HISTORY_INVALID",
                    "Refresh token history data is invalid");

    public static final UserErrorCode OAUTH2_REFRESH_TOKEN_HASH_INVALID =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_OAUTH2_REFRESH_TOKEN_HASH_INVALID",
                    "Refresh token hash is invalid");

    public static final UserErrorCode OAUTH2_REFRESH_TOKEN_EXPIRATION_INVALID =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_OAUTH2_REFRESH_TOKEN_EXPIRATION_INVALID",
                    "Refresh token expiration is invalid");

    public static final UserErrorCode OAUTH2_REFRESH_TOKEN_TIMESTAMP_REQUIRED =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_OAUTH2_REFRESH_TOKEN_TIMESTAMP_REQUIRED",
                    "Refresh token transition timestamp is required");

    public static final UserErrorCode OAUTH2_REFRESH_TOKEN_TRANSITION_NOT_ALLOWED =
            new UserErrorCode(
                    ErrorCategory.BUSINESS,
                    "USER_OAUTH2_REFRESH_TOKEN_TRANSITION_NOT_ALLOWED",
                    "Refresh token state transition is not allowed");

    public static final UserErrorCode OAUTH2_REFRESH_TOKEN_CRYPTO_FAILURE =
            new UserErrorCode(
                    ErrorCategory.SYSTEM,
                    "USER_OAUTH2_REFRESH_TOKEN_CRYPTO_FAILURE",
                    "Refresh token cryptography is unavailable");

    public static final UserErrorCode OAUTH2_REFRESH_TOKEN_HISTORY_NOT_FOUND =
            new UserErrorCode(
                    ErrorCategory.SYSTEM,
                    "USER_OAUTH2_REFRESH_TOKEN_HISTORY_NOT_FOUND",
                    "Refresh token history is unavailable");

    public static final UserErrorCode OAUTH2_CLIENT_NOT_FOUND =
            new UserErrorCode(
                    ErrorCategory.RESOURCE,
                    "USER_OAUTH2_CLIENT_NOT_FOUND",
                    "OAuth2 client not found");

    public static final UserErrorCode OAUTH2_CLIENT_ALREADY_INACTIVE =
            new UserErrorCode(
                    ErrorCategory.BUSINESS,
                    "USER_OAUTH2_CLIENT_ALREADY_INACTIVE",
                    "OAuth2 client is already inactive");

    public static final UserErrorCode OAUTH2_CLIENT_SECRET_ROTATION_NOT_ALLOWED =
            new UserErrorCode(
                    ErrorCategory.BUSINESS,
                    "USER_OAUTH2_CLIENT_SECRET_ROTATION_NOT_ALLOWED",
                    "OAuth2 client does not support secret rotation");

    public static final UserErrorCode USER_ID_REQUIRED =
            new UserErrorCode(
                    ErrorCategory.VALIDATION, "USER_ID_REQUIRED", "User identifier is required");

    public static final UserErrorCode OAUTH2_REVOCATION_AUDIT_INVALID =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_OAUTH2_REVOCATION_AUDIT_INVALID",
                    "OAuth2 revocation audit data is invalid");

    public static final UserErrorCode SECURITY_AUDIT_EVENT_INVALID =
            new UserErrorCode(
                    ErrorCategory.VALIDATION,
                    "USER_SECURITY_AUDIT_EVENT_INVALID",
                    "Security audit event data is invalid");

    final ErrorCategory category;
    private final String code;
    private final String message;

    private UserErrorCode(ErrorCategory category, String code, String message) {

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
