package com.cinema.inventory.config;

import java.time.Instant;
import java.util.List;

import com.cinema.common.security.jwt.CinemaJwtAuthenticationConverter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class InventorySecurityConfigTest {

    private static final String USER_ID = "019c1234-1111-7abc-8def-0123456789ab";

    @Autowired
    private CinemaJwtAuthenticationConverter converter;

    @Test
    void shouldLoadSharedJwtAuthenticationConverter() {
        assertThat(converter).isNotNull();
    }

    @Test
    void shouldUseSharedAuthorityMapping() {
        AbstractAuthenticationToken authentication = converter.convert(jwt());

        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(USER_ID);

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder(
                        "ROLE_SERVICE",
                        "inventory:manage");
    }

    private Jwt jwt() {
        Instant now = Instant.now();

        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(USER_ID)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("roles", List.of("SERVICE"))
                .claim(
                        "permissions",
                        List.of("inventory:manage"))
                .build();
    }
}
