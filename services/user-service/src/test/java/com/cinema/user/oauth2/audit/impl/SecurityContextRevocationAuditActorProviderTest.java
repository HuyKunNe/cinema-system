package com.cinema.user.oauth2.audit.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.cinema.common.core.constant.CommonConstants;
import com.cinema.common.security.authentication.AuthenticationUser;
import com.cinema.user.enums.AccountStatus;
import com.cinema.user.oauth2.audit.RevocationAuditActor;
import com.cinema.user.security.CinemaUserDetails;

class SecurityContextRevocationAuditActorProviderTest {

    private static final UUID USER_ID = UUID.fromString(
            "019c5000-0000-7000-8000-000000000721");

    private static final String USERNAME = "audit-admin";

    private final SecurityContextRevocationAuditActorProvider provider = new SecurityContextRevocationAuditActorProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void missingAuthenticationShouldReturnSystemActor() {
        RevocationAuditActor actor = provider.currentActor();

        assertSystemActor(
                actor);
    }

    @Test
    void anonymousAuthenticationShouldReturnSystemActor() {
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_ANONYMOUS")));

        SecurityContextHolder.getContext()
                .setAuthentication(
                        authentication);

        RevocationAuditActor actor = provider.currentActor();

        assertSystemActor(
                actor);
    }

    @Test
    void cinemaUserDetailsShouldResolveUserActor() {
        CinemaUserDetails principal = new CinemaUserDetails(
                USER_ID,
                USERNAME,
                null,
                AccountStatus.ACTIVE,
                List.of(
                        new SimpleGrantedAuthority(
                                "user:manage")));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities());

        SecurityContextHolder.getContext()
                .setAuthentication(
                        authentication);

        RevocationAuditActor actor = provider.currentActor();

        assertThat(actor.userId())
                .isEqualTo(
                        USER_ID);

        assertThat(actor.name())
                .isEqualTo(
                        USERNAME);
    }

    @Test
    void authenticationUserShouldResolveUserActor() {
        AuthenticationUser principal = new AuthenticationUser(
                USER_ID,
                USERNAME,
                Set.of(
                        "ADMIN"),
                Set.of(
                        "user:manage"));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(
                        new SimpleGrantedAuthority(
                                "user:manage")));

        SecurityContextHolder.getContext()
                .setAuthentication(
                        authentication);

        RevocationAuditActor actor = provider.currentActor();

        assertThat(actor.userId())
                .isEqualTo(
                        USER_ID);

        assertThat(actor.name())
                .isEqualTo(
                        USERNAME);
    }

    @Test
    void jwtUserShouldResolveUuidSubjectAndUsernameClaim() {
        Jwt jwt = jwt(
                USER_ID.toString(),
                Map.of(
                        "username",
                        USERNAME));

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                List.of(
                        new SimpleGrantedAuthority(
                                "user:manage")),
                USERNAME);

        SecurityContextHolder.getContext()
                .setAuthentication(
                        authentication);

        RevocationAuditActor actor = provider.currentActor();

        assertThat(actor.userId())
                .isEqualTo(
                        USER_ID);

        assertThat(actor.name())
                .isEqualTo(
                        USERNAME);
    }

    @Test
    void jwtServiceShouldUseAuthenticationNameWithoutUserId() {
        String serviceClientId = "inventory-service";

        Jwt jwt = jwt(
                serviceClientId,
                Map.of());

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                List.of(
                        new SimpleGrantedAuthority(
                                "inventory:manage")),
                serviceClientId);

        SecurityContextHolder.getContext()
                .setAuthentication(
                        authentication);

        RevocationAuditActor actor = provider.currentActor();

        assertThat(actor.userId())
                .isNull();

        assertThat(actor.name())
                .isEqualTo(
                        serviceClientId);
    }

    @Test
    void standardUserDetailsShouldUseUsernameWithoutUserId() {
        User principal = new User(
                USERNAME,
                "password",
                List.of(
                        new SimpleGrantedAuthority(
                                "user:manage")));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities());

        SecurityContextHolder.getContext()
                .setAuthentication(
                        authentication);

        RevocationAuditActor actor = provider.currentActor();

        assertThat(actor.userId())
                .isNull();

        assertThat(actor.name())
                .isEqualTo(
                        USERNAME);
    }

    @Test
    void unsupportedAuthenticatedPrincipalShouldUseAuthenticationName() {
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                new Object(),
                null,
                List.of(
                        new SimpleGrantedAuthority(
                                "user:manage")));

        SecurityContextHolder.getContext()
                .setAuthentication(
                        authentication);

        RevocationAuditActor actor = provider.currentActor();

        assertThat(actor.userId())
                .isNull();

        assertThat(actor.name())
                .isNotBlank();
    }

    private static Jwt jwt(
            String subject,
            Map<String, Object> claims) {

        Jwt.Builder builder = Jwt.withTokenValue(
                "audit-token")
                .header(
                        "alg",
                        "none")
                .subject(
                        subject)
                .issuedAt(
                        Instant.parse(
                                "2026-08-13T10:00:00Z"))
                .expiresAt(
                        Instant.parse(
                                "2026-08-13T10:15:00Z"));

        claims.forEach(
                builder::claim);

        return builder.build();
    }

    private static void assertSystemActor(
            RevocationAuditActor actor) {

        assertThat(actor.userId())
                .isNull();

        assertThat(actor.name())
                .isEqualTo(
                        CommonConstants.SYSTEM);
    }
}
