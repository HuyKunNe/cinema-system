package com.cinema.inventory.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import com.cinema.common.security.jwt.CinemaJwtAuthenticationConverter;

@Configuration
@EnableWebSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class InventorySecurityConfig {

    @Bean
    public SecurityFilterChain inventorySecurityFilterChain(
            HttpSecurity http,
            CinemaJwtAuthenticationConverter jwtAuthenticationConverter)
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

    /*
     * Temporary HS256 decoder.
     *
     * This bean remains only until the shared issuer/JWK decoder and
     * audience validation are implemented in the next checkpoints.
     */
    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${cinema.security.jwt.secret}") String secret) {

        SecretKey secretKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");

        return NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
