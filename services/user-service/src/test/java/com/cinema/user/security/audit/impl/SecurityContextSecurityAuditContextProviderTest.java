package com.cinema.user.security.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.core.constant.CommonConstants;
import com.cinema.common.security.authentication.AuthenticationUser;
import com.cinema.user.enums.AccountStatus;
import com.cinema.user.security.CinemaUserDetails;
import com.cinema.user.security.audit.SecurityAuditActorType;
import com.cinema.user.security.audit.SecurityAuditContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

class SecurityContextSecurityAuditContextProviderTest {

    private static final UUID USER_ID = UUID.fromString("019c5000-0000-7000-8000-000000000731");

    private static final String USERNAME = "security-audit-admin";

    private static final String CORRELATION_ID = "request-019c5000";

    private static final String TRACE_ID = "trace-019c5000";

    private final SecurityContextSecurityAuditContextProvider provider =
            new SecurityContextSecurityAuditContextProvider();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void missingAuthenticationShouldReturnSystemActor() {
        SecurityAuditContext context = provider.currentContext();

        assertSystemActor(context);

        assertThat(context.correlationId()).isNull();
    }

    @Test
    void anonymousAuthenticationShouldReturnSystemActor() {
        AnonymousAuthenticationToken authentication =
                new AnonymousAuthenticationToken(
                        "anonymous-key",
                        "anonymousUser",
                        List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityAuditContext context = provider.currentContext();

        assertSystemActor(context);
    }

    @Test
    void unauthenticatedTokenShouldReturnSystemActor() {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.unauthenticated(USERNAME, null);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityAuditContext context = provider.currentContext();

        assertSystemActor(context);
    }

    @Test
    void cinemaUserDetailsShouldResolveUserActor() {
        CinemaUserDetails principal =
                new CinemaUserDetails(
                        USER_ID,
                        USERNAME,
                        null,
                        AccountStatus.ACTIVE,
                        List.of(new SimpleGrantedAuthority("user:manage")));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityAuditContext context = provider.currentContext();

        assertUserActor(context, USERNAME);
    }

    @Test
    void authenticationUserShouldResolveUserActor() {
        AuthenticationUser principal =
                new AuthenticationUser(USER_ID, USERNAME, Set.of("ADMIN"), Set.of("user:manage"));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("user:manage")));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityAuditContext context = provider.currentContext();

        assertUserActor(context, USERNAME);
    }

    @Test
    void authenticationUserWithoutUsernameShouldUseAuthenticationName() {
        AuthenticationUser principal = new AuthenticationUser(USER_ID, null, Set.of(), Set.of());

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, List.of(new SimpleGrantedAuthority("user:manage")));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityAuditContext context = provider.currentContext();

        assertThat(context.actor().type()).isEqualTo(SecurityAuditActorType.USER);

        assertThat(context.actor().reference()).isNotBlank();
    }

    @Test
    void jwtAuthenticationShouldUseUsernameClaim() {
        Jwt jwt = jwt(USER_ID.toString(), Map.of("username", USERNAME));

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(
                        jwt, List.of(new SimpleGrantedAuthority("user:manage")), USERNAME);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityAuditContext context = provider.currentContext();

        assertUserActor(context, USERNAME);
    }

    @Test
    void jwtAuthenticationWithoutUsernameShouldUseAuthenticationName() {
        String clientName = "inventory-service";

        Jwt jwt = jwt(clientName, Map.of());

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(
                        jwt, List.of(new SimpleGrantedAuthority("inventory:manage")), clientName);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityAuditContext context = provider.currentContext();

        assertUserActor(context, clientName);
    }

    @Test
    void jwtPrincipalShouldUseUsernameClaim() {
        Jwt principal = jwt(USER_ID.toString(), Map.of("username", USERNAME));

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, List.of(new SimpleGrantedAuthority("user:manage")));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityAuditContext context = provider.currentContext();

        assertUserActor(context, USERNAME);
    }

    @Test
    void standardUserDetailsShouldUseUsername() {
        User principal =
                new User(USERNAME, "password", List.of(new SimpleGrantedAuthority("user:manage")));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityAuditContext context = provider.currentContext();

        assertUserActor(context, USERNAME);
    }

    @Test
    void unsupportedPrincipalShouldUseAuthenticationName() {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        USERNAME, null, List.of(new SimpleGrantedAuthority("user:manage")));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        SecurityAuditContext context = provider.currentContext();

        assertUserActor(context, USERNAME);
    }

    @Test
    void correlationIdShouldTakePrecedenceOverTraceId() {
        MDC.put("correlationId", "  " + CORRELATION_ID + "  ");

        MDC.put("traceId", TRACE_ID);

        SecurityAuditContext context = provider.currentContext();

        assertThat(context.correlationId()).isEqualTo(CORRELATION_ID);
    }

    @Test
    void traceIdShouldBeUsedWhenCorrelationIdIsMissing() {
        MDC.put("traceId", "  " + TRACE_ID + "  ");

        SecurityAuditContext context = provider.currentContext();

        assertThat(context.correlationId()).isEqualTo(TRACE_ID);
    }

    @Test
    void blankCorrelationIdShouldFallBackToTraceId() {
        MDC.put("correlationId", "   ");

        MDC.put("traceId", TRACE_ID);

        SecurityAuditContext context = provider.currentContext();

        assertThat(context.correlationId()).isEqualTo(TRACE_ID);
    }

    private static Jwt jwt(String subject, Map<String, Object> claims) {

        Jwt.Builder builder =
                Jwt.withTokenValue("security-audit-token")
                        .header("alg", "none")
                        .subject(subject)
                        .issuedAt(Instant.parse("2026-08-17T10:00:00Z"))
                        .expiresAt(Instant.parse("2026-08-17T10:15:00Z"));

        claims.forEach(builder::claim);

        return builder.build();
    }

    private static void assertSystemActor(SecurityAuditContext context) {

        assertThat(context.actor().type()).isEqualTo(SecurityAuditActorType.SYSTEM);

        assertThat(context.actor().reference()).isEqualTo(CommonConstants.SYSTEM);
    }

    private static void assertUserActor(SecurityAuditContext context, String expectedReference) {

        assertThat(context.actor().type()).isEqualTo(SecurityAuditActorType.USER);

        assertThat(context.actor().reference()).isEqualTo(expectedReference);
    }
}
