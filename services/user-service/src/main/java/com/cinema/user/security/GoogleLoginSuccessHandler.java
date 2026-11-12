package com.cinema.user.security;

import com.cinema.user.security.audit.impl.SecurityAuditAuthenticationFailureHandler;
import com.cinema.user.service.GoogleIdentityService;
import com.cinema.user.service.GoogleIdentityService.GoogleAccount;
import com.cinema.user.service.UserAccountLifecycleService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final GoogleIdentityService googleIdentityService;
    private final UserDetailsService userDetailsService;
    private final UserAccountLifecycleService accountLifecycleService;
    private final AuthenticationSuccessHandler auditSuccessHandler;
    private final SecurityAuditAuthenticationFailureHandler auditFailureHandler;
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public GoogleLoginSuccessHandler(
            GoogleIdentityService googleIdentityService,
            UserDetailsService userDetailsService,
            UserAccountLifecycleService accountLifecycleService,
            com.cinema.user.security.audit.impl.SecurityAuditAuthenticationSuccessHandler
                    auditSuccessHandler,
            SecurityAuditAuthenticationFailureHandler auditFailureHandler) {
        this.googleIdentityService = googleIdentityService;
        this.userDetailsService = userDetailsService;
        this.accountLifecycleService = accountLifecycleService;
        this.auditSuccessHandler = auditSuccessHandler;
        this.auditFailureHandler = auditFailureHandler;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        try {
            if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)
                    || oidcUser.getIdToken().getIssuer() == null) {
                throw invalidIdentity();
            }

            String issuer = oidcUser.getIdToken().getIssuer().toString();
            String subject = oidcUser.getSubject();
            String email = oidcUser.getEmail();
            Boolean emailVerified = oidcUser.getEmailVerified();

            GoogleAccount account;
            try {
                account = resolve(issuer, subject, email, emailVerified, oidcUser);
            } catch (DataIntegrityViolationException concurrentIdentityCreation) {
                account = resolve(issuer, subject, email, emailVerified, oidcUser);
            }

            UserDetails loaded = userDetailsService.loadUserByUsername(account.username());
            if (!(loaded instanceof CinemaUserDetails userDetails) || !userDetails.isEnabled()) {
                throw invalidIdentity();
            }

            accountLifecycleService.recordSuccessfulLogin(userDetails.getUserId());
            Authentication localAuthentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            userDetails, null, userDetails.getAuthorities());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(localAuthentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
            auditSuccessHandler.onAuthenticationSuccess(request, response, localAuthentication);
        } catch (AuthenticationException exception) {
            auditFailureHandler.onAuthenticationFailure(request, response, exception);
        } catch (DataIntegrityViolationException exception) {
            auditFailureHandler.onAuthenticationFailure(request, response, invalidIdentity());
        }
    }

    private GoogleAccount resolve(
            String issuer, String subject, String email, Boolean emailVerified, OidcUser oidcUser) {
        return googleIdentityService.resolveOrCreate(
                issuer,
                subject,
                email,
                emailVerified,
                oidcUser.getGivenName(),
                oidcUser.getFamilyName());
    }

    private static OAuth2AuthenticationException invalidIdentity() {
        return new OAuth2AuthenticationException(
                new OAuth2Error("google_identity_invalid"),
                "Google identity cannot be used to sign in");
    }
}
