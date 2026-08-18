package com.cinema.movie.config;

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
public class MovieSecurityConfig {

    private static final String[] CATALOG_ENDPOINTS = {
        "/api/v1/movies", "/api/v1/movies/**", "/api/v1/genres", "/api/v1/genres/**"
    };

    @Bean
    SecurityFilterChain movieSecurityFilterChain(
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
                                        .requestMatchers(HttpMethod.GET, CATALOG_ENDPOINTS)
                                        .permitAll()
                                        .requestMatchers(CATALOG_ENDPOINTS)
                                        .hasAuthority("movie:manage")
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
