package com.cinema.inventory.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InventorySecurityConfigTest {

    private final InventorySecurityConfig securityConfig = new InventorySecurityConfig();

    private final Converter<Jwt, AbstractAuthenticationToken> converter = securityConfig.jwtAuthenticationConverter();

    @Test
    void shouldMapServiceRole() {
        AbstractAuthenticationToken authentication = convert(jwt(
                "service-account",
                List.of("SERVICE"),
                List.of()));

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_SERVICE");
    }

    @Test
    void shouldNotDuplicateRolePrefix() {
        AbstractAuthenticationToken authentication = convert(jwt(
                "service-account",
                List.of("ROLE_SERVICE"),
                List.of()));

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_SERVICE");
    }

    @Test
    void shouldMapInventoryManagePermission() {
        AbstractAuthenticationToken authentication = convert(jwt(
                "administrator",
                List.of(),
                List.of("inventory:manage")));

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("inventory:manage");
    }

    @Test
    void shouldMapRolesAndPermissionsTogether() {
        AbstractAuthenticationToken authentication = convert(jwt(
                "inventory-service",
                List.of("SERVICE", "USER"),
                List.of(
                        "inventory:manage",
                        "inventory:read")));

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly(
                        "ROLE_SERVICE",
                        "ROLE_USER",
                        "inventory:manage",
                        "inventory:read");
    }

    @Test
    void shouldTrimAndRemoveBlankAuthorities() {
        AbstractAuthenticationToken authentication = convert(jwt(
                "inventory-service",
                List.of(" SERVICE ", " ", ""),
                List.of(
                        " inventory:manage ",
                        " ",
                        "")));

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly(
                        "ROLE_SERVICE",
                        "inventory:manage");
    }

    @Test
    void shouldRemoveDuplicateAuthorities() {
        AbstractAuthenticationToken authentication = convert(jwt(
                "inventory-service",
                List.of(
                        "SERVICE",
                        "ROLE_SERVICE",
                        "SERVICE"),
                List.of(
                        "inventory:manage",
                        "inventory:manage")));

        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly(
                        "ROLE_SERVICE",
                        "inventory:manage");
    }

    @Test
    void shouldReturnNoAuthoritiesWhenClaimsAreMissing() {
        AbstractAuthenticationToken authentication = convert(jwtWithoutAuthorities("user-id"));

        assertThat(authentication.getAuthorities())
                .isEmpty();
    }

    @Test
    void shouldUseSubjectAsPrincipalName() {
        AbstractAuthenticationToken authentication = convert(jwt(
                "inventory-service",
                List.of("SERVICE"),
                List.of()));

        assertThat(authentication.getName())
                .isEqualTo("inventory-service");
    }

    private AbstractAuthenticationToken convert(Jwt jwt) {
        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication).isNotNull();

        return authentication;
    }

    private Jwt jwt(
            String subject,
            List<String> roles,
            List<String> permissions) {

        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("roles", roles)
                .claim("permissions", permissions)
                .build();
    }

    private Jwt jwtWithoutAuthorities(String subject) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claims(claims -> claims.putAll(Map.of()))
                .build();
    }
}
