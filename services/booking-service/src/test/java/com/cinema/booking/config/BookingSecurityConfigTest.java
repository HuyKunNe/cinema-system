package com.cinema.booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.security.jwt.CinemaJwtAuthenticationConverter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

@ActiveProfiles("test")
@SpringBootTest
class BookingSecurityConfigTest {

    private static final String USER_ID = "019c1234-1111-7abc-8def-0123456789ab";

    @Autowired private CinemaJwtAuthenticationConverter converter;

    @Autowired private SecurityFilterChain securityFilterChain;

    @Test
    void shouldLoadBookingSecurityFilterChain() {
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    void shouldLoadSharedJwtAuthenticationConverter() {
        assertThat(converter).isNotNull();
    }

    @Test
    void shouldUseJwtSubjectAsAuthenticatedPrincipal() {
        AbstractAuthenticationToken authentication = converter.convert(jwt());

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(USER_ID);

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "booking:read");
    }

    private Jwt jwt() {
        Instant now = Instant.now();

        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(USER_ID)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("roles", List.of("USER"))
                .claim("permissions", List.of("booking:read"))
                .build();
    }
}
