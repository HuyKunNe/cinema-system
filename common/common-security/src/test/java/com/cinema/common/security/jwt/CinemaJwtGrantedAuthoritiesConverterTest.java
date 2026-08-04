package com.cinema.common.security.jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class CinemaJwtGrantedAuthoritiesConverterTest {

    private final CinemaJwtGrantedAuthoritiesConverter converter = new CinemaJwtGrantedAuthoritiesConverter();

    @Test
    void shouldConvertRolesAndPermissions() {
        Jwt jwt = jwt(
                List.of("USER", "ROLE_ADMIN"),
                List.of("booking:create", "inventory:manage"));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorityNames(authorities))
                .containsExactlyInAnyOrder(
                        "ROLE_USER",
                        "ROLE_ADMIN",
                        "booking:create",
                        "inventory:manage");
    }

    @Test
    void shouldRemoveBlankAndDuplicateAuthorities() {
        Jwt jwt = jwt(
                List.of(
                        "USER",
                        " USER ",
                        "",
                        " ",
                        "ROLE_USER"),
                List.of(
                        "booking:create",
                        " booking:create ",
                        "",
                        " "));

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorityNames(authorities))
                .containsExactlyInAnyOrder(
                        "ROLE_USER",
                        "booking:create");
    }

    @Test
    void shouldReturnEmptyAuthoritiesWhenClaimsAreMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("019c1234-1111-7abc-8def-0123456789ab")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void shouldIgnoreClaimsWithUnsupportedType() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("019c1234-1111-7abc-8def-0123456789ab")
                .claim("roles", "USER")
                .claim("permissions", 123)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(900))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities).isEmpty();
    }

    private Jwt jwt(
            List<String> roles,
            List<String> permissions) {

        Instant now = Instant.now();

        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("019c1234-1111-7abc-8def-0123456789ab")
                .claim("username", "user@example.com")
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(900))
                .build();
    }

    private Set<String> authorityNames(
            Collection<GrantedAuthority> authorities) {

        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
