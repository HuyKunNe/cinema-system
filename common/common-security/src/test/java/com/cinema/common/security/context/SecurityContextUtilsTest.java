package com.cinema.common.security.context;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.cinema.common.exception.exception.UnauthorizedException;
import com.cinema.common.security.authentication.AuthenticationUser;
import com.cinema.common.security.error.SecurityErrorCode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityContextUtilsTest {

    private static final UUID USER_ID = UUID.fromString(
            "019c1234-1111-7abc-8def-0123456789ab");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnAuthenticationUserPrincipal() {
        AuthenticationUser expected = new AuthenticationUser(
                USER_ID,
                "user@example.com",
                Set.of("USER"),
                Set.of("booking:create"));

        setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        expected,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_USER"))));

        AuthenticationUser actual = SecurityContextUtils.getCurrentUser();

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldCreateAuthenticationUserFromJwt() {
        setAuthentication(
                authenticatedJwt(validJwt()));

        AuthenticationUser user = SecurityContextUtils.getCurrentUser();

        assertThat(user.userId()).isEqualTo(USER_ID);
        assertThat(user.username())
                .isEqualTo("user@example.com");

        assertThat(user.roles())
                .containsExactly("USER");

        assertThat(user.permissions())
                .containsExactly("booking:create");
    }

    @Test
    void shouldRemoveBlankAndDuplicateJwtClaims() {
        Jwt jwt = jwt(
                USER_ID.toString(),
                List.of("USER", " USER ", "", " "),
                List.of(
                        "booking:create",
                        " booking:create ",
                        "",
                        " "));

        setAuthentication(authenticatedJwt(jwt));

        AuthenticationUser user = SecurityContextUtils.getCurrentUser();

        assertThat(user.roles())
                .containsExactly("USER");

        assertThat(user.permissions())
                .containsExactly("booking:create");
    }

    @Test
    void shouldReturnEmptySetsWhenAuthorityClaimsAreMissing() {
        Instant now = Instant.now();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(USER_ID.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();

        setAuthentication(authenticatedJwt(jwt));

        AuthenticationUser user = SecurityContextUtils.getCurrentUser();

        assertThat(user.roles()).isEmpty();
        assertThat(user.permissions()).isEmpty();
    }

    @Test
    void shouldRejectMissingAuthentication() {
        assertUnauthorized(
                SecurityContextUtils::getCurrentUser,
                SecurityErrorCode.AUTHENTICATION_REQUIRED);
    }

    @Test
    void shouldRejectAnonymousAuthentication() {
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
                "anonymous-key",
                "anonymousUser",
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_ANONYMOUS")));

        setAuthentication(authentication);

        assertUnauthorized(
                SecurityContextUtils::getCurrentUser,
                SecurityErrorCode.AUTHENTICATION_REQUIRED);
    }

    @Test
    void shouldRejectUnsupportedPrincipal() {
        setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "unsupported-principal",
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_USER"))));

        assertUnauthorized(
                SecurityContextUtils::getCurrentUser,
                SecurityErrorCode.UNSUPPORTED_AUTHENTICATED_PRINCIPAL);
    }

    @Test
    void shouldRejectMissingJwtSubject() {
        Instant now = Instant.now();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();

        setAuthentication(authenticatedJwt(jwt));

        assertUnauthorized(
                SecurityContextUtils::getCurrentUser,
                SecurityErrorCode.INVALID_JWT_SUBJECT);
    }

    @Test
    void shouldRejectInvalidJwtSubject() {
        Jwt jwt = jwt(
                "not-a-uuid",
                List.of("USER"),
                List.of());

        setAuthentication(authenticatedJwt(jwt));

        assertUnauthorized(
                SecurityContextUtils::getCurrentUser,
                SecurityErrorCode.INVALID_JWT_SUBJECT);
    }

    private Jwt validJwt() {
        return jwt(
                USER_ID.toString(),
                List.of("USER"),
                List.of("booking:create"));
    }

    private Jwt jwt(
            String subject,
            List<String> roles,
            List<String> permissions) {

        Instant now = Instant.now();

        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(subject)
                .claim("username", "user@example.com")
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();
    }

    private void setAuthentication(
            org.springframework.security.core.Authentication authentication) {

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);
    }

    private void assertUnauthorized(
            Runnable operation,
            SecurityErrorCode expectedErrorCode) {

        assertThatThrownBy(operation::run)
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(exception -> {
                    UnauthorizedException actual = (UnauthorizedException) exception;

                    assertThat(actual.getErrorCode())
                            .isEqualTo(expectedErrorCode);
                });
    }

    private JwtAuthenticationToken authenticatedJwt(Jwt jwt) {
        return new JwtAuthenticationToken(
                jwt,
                List.of(),
                jwt.getSubject());
    }
}
