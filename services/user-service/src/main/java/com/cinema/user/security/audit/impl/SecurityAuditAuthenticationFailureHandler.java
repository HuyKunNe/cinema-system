package com.cinema.user.security.audit.impl;

import java.io.IOException;

import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditRecord;
import com.cinema.user.security.audit.SecurityAuditRecorder;
import com.cinema.user.security.audit.SecurityAuditTargetType;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityAuditAuthenticationFailureHandler
        implements AuthenticationFailureHandler {

    private static final int USER_REFERENCE_MAX_LENGTH =
            200;

    private final SecurityAuditRecorder securityAuditRecorder;

    private final AuthenticationFailureHandler delegate =
            new SimpleUrlAuthenticationFailureHandler(
                    "/login?error");

    public SecurityAuditAuthenticationFailureHandler(
            SecurityAuditRecorder securityAuditRecorder) {

        this.securityAuditRecorder =
                securityAuditRecorder;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception)
            throws IOException, ServletException {

        String attemptedUsername =
                attemptedUsername(
                        request);

        securityAuditRecorder.record(
                new SecurityAuditRecord(
                        SecurityAuditEventType.AUTHENTICATION_FAILED,
                        attemptedUsername == null
                                ? null
                                : SecurityAuditTargetType.USER,
                        attemptedUsername,
                        SecurityAuditOutcome.FAILURE,
                        failureReason(
                                exception),
                        null));

        delegate.onAuthenticationFailure(
                request,
                response,
                exception);
    }

    private static String attemptedUsername(
            HttpServletRequest request) {

        String value =
                request.getParameter(
                        "username");

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        if (normalized.isEmpty()
                || normalized.length()
                        > USER_REFERENCE_MAX_LENGTH) {

            return null;
        }

        return normalized;
    }

    private static String failureReason(
            AuthenticationException exception) {

        if (exception instanceof LockedException) {
            return "ACCOUNT_LOCKED";
        }

        if (exception instanceof DisabledException) {
            return "ACCOUNT_DISABLED";
        }

        if (exception instanceof AccountExpiredException) {
            return "ACCOUNT_EXPIRED";
        }

        if (exception instanceof CredentialsExpiredException) {
            return "CREDENTIALS_EXPIRED";
        }

        if (exception instanceof BadCredentialsException) {
            return "BAD_CREDENTIALS";
        }

        return "AUTHENTICATION_FAILED";
    }
}
