package com.cinema.inventory.config;

import com.cinema.common.security.jwt.CinemaJwtAuthenticationConverter;
import com.cinema.common.security.web.CinemaAccessDeniedHandler;
import com.cinema.common.security.web.CinemaAuthenticationEntryPoint;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class InventorySecurityConfig {

    private static final String[] PUBLIC_INVENTORY_ENDPOINTS = {
        "/api/v1/cinemas",
        "/api/v1/cinemas/**",
        "/api/v1/rooms",
        "/api/v1/rooms/**",
        "/api/v1/seats",
        "/api/v1/seats/**",
        "/api/v1/showtimes",
        "/api/v1/showtimes/**",
        "/api/v1/show-seats",
        "/api/v1/show-seats/**"
    };

    private static final String[] INVENTORY_MANAGEMENT_ENDPOINTS = {
        "/api/v1/cinemas",
        "/api/v1/cinemas/**",
        "/api/v1/rooms",
        "/api/v1/rooms/**",
        "/api/v1/seats",
        "/api/v1/seats/**"
    };

    private static final String[] SHOWTIME_MANAGEMENT_ENDPOINTS = {
        "/api/v1/showtimes", "/api/v1/showtimes/**"
    };

    @Bean
    SecurityFilterChain inventorySecurityFilterChain(
            HttpSecurity http,
            CinemaJwtAuthenticationConverter jwtAuthenticationConverter,
            CinemaAuthenticationEntryPoint authenticationEntryPoint,
            CinemaAccessDeniedHandler accessDeniedHandler)
            throws Exception {

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        .requestMatchers("/actuator/health", "/actuator/info")
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, PUBLIC_INVENTORY_ENDPOINTS)
                                        .permitAll()
                                        .requestMatchers(HttpMethod.POST, "/api/v1/show-seats")
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
                                        .requestMatchers(SHOWTIME_MANAGEMENT_ENDPOINTS)
                                        .hasAuthority("showtime:manage")
                                        .requestMatchers(INVENTORY_MANAGEMENT_ENDPOINTS)
                                        .hasAuthority("inventory:manage")
                                        .anyRequest()
                                        .denyAll())
                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        .authenticationEntryPoint(authenticationEntryPoint)
                                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2.authenticationEntryPoint(authenticationEntryPoint)
                                        .accessDeniedHandler(accessDeniedHandler)
                                        .jwt(
                                                jwt ->
                                                        jwt.jwtAuthenticationConverter(
                                                                jwtAuthenticationConverter)));

        return http.build();
    }
}
