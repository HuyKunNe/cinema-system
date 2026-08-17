package com.cinema.user.security.audit.impl;

import com.cinema.common.core.constant.CommonConstants;
import com.cinema.common.security.authentication.AuthenticationUser;
import com.cinema.common.security.constant.SecurityConstants;
import com.cinema.user.security.CinemaUserDetails;
import com.cinema.user.security.audit.SecurityAuditActor;
import com.cinema.user.security.audit.SecurityAuditActorType;
import com.cinema.user.security.audit.SecurityAuditContext;
import com.cinema.user.security.audit.SecurityAuditContextProvider;

import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextSecurityAuditContextProvider implements SecurityAuditContextProvider {

    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    public SecurityAuditContext currentContext() {
        return new SecurityAuditContext(currentActor(), currentCorrelationId());
    }

    private static SecurityAuditActor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            return systemActor();
        }

        if (authentication instanceof OAuth2ClientAuthenticationToken clientAuthentication) {

            return clientActor(clientAuthentication);
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CinemaUserDetails userDetails) {

            return new SecurityAuditActor(SecurityAuditActorType.USER, userDetails.getUsername());
        }

        if (principal instanceof AuthenticationUser user) {

            return new SecurityAuditActor(
                    SecurityAuditActorType.USER,
                    actorReference(user.username(), authentication.getName()));
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {

            return userActor(jwtAuthentication.getToken(), authentication.getName());
        }

        if (principal instanceof Jwt jwt) {
            return userActor(jwt, authentication.getName());
        }

        if (principal instanceof UserDetails userDetails) {

            return new SecurityAuditActor(
                    SecurityAuditActorType.USER,
                    actorReference(userDetails.getUsername(), authentication.getName()));
        }

        return new SecurityAuditActor(
                SecurityAuditActorType.USER,
                actorReference(authentication.getName(), CommonConstants.SYSTEM));
    }

    private static SecurityAuditActor clientActor(OAuth2ClientAuthenticationToken authentication) {

        if (authentication.getRegisteredClient() != null) {
            return new SecurityAuditActor(
                    SecurityAuditActorType.CLIENT,
                    authentication.getRegisteredClient().getClientId());
        }

        return new SecurityAuditActor(
                SecurityAuditActorType.CLIENT,
                actorReference(authentication.getName(), CommonConstants.SYSTEM));
    }

    private static SecurityAuditActor userActor(Jwt jwt, String authenticationName) {

        String username = jwt.getClaimAsString(SecurityConstants.CLAIM_USERNAME);

        return new SecurityAuditActor(
                SecurityAuditActorType.USER, actorReference(username, authenticationName));
    }

    private static String currentCorrelationId() {
        String correlationId = normalizedMdcValue(CORRELATION_ID_MDC_KEY);

        if (correlationId != null) {
            return correlationId;
        }

        return normalizedMdcValue(TRACE_ID_MDC_KEY);
    }

    private static String normalizedMdcValue(String key) {

        String value = MDC.get(key);

        if (value == null || value.isBlank()) {

            return null;
        }

        return value.trim();
    }

    private static String actorReference(String preferred, String fallback) {

        if (preferred != null && !preferred.isBlank()) {

            return preferred.trim();
        }

        if (fallback != null && !fallback.isBlank()) {

            return fallback.trim();
        }

        return CommonConstants.SYSTEM;
    }

    private static SecurityAuditActor systemActor() {
        return new SecurityAuditActor(SecurityAuditActorType.SYSTEM, CommonConstants.SYSTEM);
    }
}
