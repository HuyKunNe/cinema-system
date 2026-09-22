package com.cinema.inventory.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.security.config.SecurityConfiguration;
import com.cinema.common.security.jwt.CinemaJwtAuthenticationConverter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

class InventorySecurityConfigTest {

    private static final String USER_ID = "019c1234-1111-7abc-8def-0123456789ab";

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(SecurityConfiguration.class))
                    .withPropertyValues("cinema.security.oauth2.audience=cinema-api");

    @Test
    void shouldLoadSharedJwtAuthenticationConverter() {

        contextRunner.run(
                context -> {
                    assertThat(context).hasSingleBean(CinemaJwtAuthenticationConverter.class);
                });
    }

    @Test
    void shouldUseSharedAuthorityMapping() {

        contextRunner.run(
                context -> {
                    CinemaJwtAuthenticationConverter converter =
                            context.getBean(CinemaJwtAuthenticationConverter.class);

                    AbstractAuthenticationToken authentication = converter.convert(jwt());

                    assertThat(authentication).isNotNull();

                    assertThat(authentication.getName()).isEqualTo(USER_ID);

                    assertThat(authentication.getAuthorities())
                            .extracting("authority")
                            .containsExactlyInAnyOrder("ROLE_SERVICE", "inventory:manage");
                });
    }

    private Jwt jwt() {

        Instant now = Instant.now();

        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(USER_ID)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("roles", List.of("SERVICE"))
                .claim("permissions", List.of("inventory:manage"))
                .build();
    }
}

