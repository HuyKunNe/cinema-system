package com.cinema.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

class CinemaJwtAuthenticationConverterTest {

    private static final String USER_ID = "019c1234-1111-7abc-8def-0123456789ab";

    private final CinemaJwtAuthenticationConverter converter =
            new CinemaJwtAuthenticationConverter();

    @Test
    void shouldUseSubjectAsPrincipalName() {
        Jwt jwt = jwt();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(USER_ID);
        assertThat(authentication.isAuthenticated()).isTrue();
    }

    @Test
    void shouldIncludeRolesAndPermissions() {
        Jwt jwt = jwt();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "booking:create");
    }

    @Test
    void shouldUseServiceClientIdAsPrincipalName() {

        Instant now = Instant.now();

        Jwt jwt =
                Jwt.withTokenValue("service-token")
                        .header("alg", "RS256")
                        .subject("booking-service")
                        .claim("permissions", List.of("inventory:write"))
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(300))
                        .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();

        assertThat(authentication.getName()).isEqualTo("booking-service");

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("inventory:write");

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .doesNotContain("ROLE_SERVICE");
    }

    private Jwt jwt() {
        Instant now = Instant.now();

        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(USER_ID)
                .claim("username", "user@example.com")
                .claim("roles", List.of("USER"))
                .claim("permissions", List.of("booking:create"))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();
    }
}
