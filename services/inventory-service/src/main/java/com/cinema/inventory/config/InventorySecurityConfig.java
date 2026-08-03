package com.cinema.inventory.config;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import static org.springframework.security.authorization.SingleResultAuthorizationManager.permitAll;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import io.jsonwebtoken.security.Keys;

@Configuration
@EnableWebSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class InventorySecurityConfig {

    @Bean
    public SecurityFilterChain inventorySecurityFilterChain(
            HttpSecurity http,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/show-seats",
                                "/api/v1/show-seats/**")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/show-seats")
                        .hasAuthority("inventory:manage")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/show-seats/*/available",
                                "/api/v1/show-seats/*/unavailable")
                        .hasAuthority("inventory:manage")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/show-seats/*/hold",
                                "/api/v1/show-seats/*/book",
                                "/api/v1/show-seats/*/release")
                        .hasRole("SERVICE")
                        .anyRequest()
                        .permitAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${cinema.security.jwt.secret}") String secret) {

        SecretKey secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8));

        return NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                this::extractAuthorities);

        converter.setPrincipalClaimName("sub");

        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(
            Jwt jwt) {

        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        addRoles(
                authorities,
                jwt.getClaimAsStringList("roles"));

        addPermissions(
                authorities,
                jwt.getClaimAsStringList("permissions"));

        return authorities;
    }

    private void addRoles(
            Set<GrantedAuthority> authorities,
            Collection<String> roles) {

        if (roles == null) {
            return;
        }

        roles.stream()
                .filter(this::hasText)
                .map(String::trim)
                .map(this::normalizeRole)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
    }

    private void addPermissions(
            Set<GrantedAuthority> authorities,
            Collection<String> permissions) {

        if (permissions == null) {
            return;
        }

        permissions.stream()
                .filter(this::hasText)
                .map(String::trim)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
    }

    private String normalizeRole(String role) {
        String normalizedRole = role.trim();

        if (normalizedRole.startsWith("ROLE_")) {
            return normalizedRole;
        }

        return "ROLE_" + normalizedRole;
    }

    private boolean hasText(String value) {
        return value != null
                && !value.isBlank();
    }
}
