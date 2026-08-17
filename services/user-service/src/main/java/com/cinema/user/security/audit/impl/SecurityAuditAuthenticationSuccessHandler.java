package com.cinema.user.security.audit.impl;

import com.cinema.user.security.audit.SecurityAuditEventType;
import com.cinema.user.security.audit.SecurityAuditOutcome;
import com.cinema.user.security.audit.SecurityAuditRecord;
import com.cinema.user.security.audit.SecurityAuditRecorder;
import com.cinema.user.security.audit.SecurityAuditTargetType;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityAuditAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final SecurityAuditRecorder securityAuditRecorder;

    private final AuthenticationSuccessHandler delegate =
            new SavedRequestAwareAuthenticationSuccessHandler();

    public SecurityAuditAuthenticationSuccessHandler(SecurityAuditRecorder securityAuditRecorder) {

        this.securityAuditRecorder = securityAuditRecorder;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        securityAuditRecorder.record(
                new SecurityAuditRecord(
                        SecurityAuditEventType.AUTHENTICATION_SUCCEEDED,
                        SecurityAuditTargetType.USER,
                        authentication.getName(),
                        SecurityAuditOutcome.SUCCESS,
                        null,
                        null));

        delegate.onAuthenticationSuccess(request, response, authentication);
    }
}
