package com.cinema.gateway.config;

import com.cinema.common.security.jwt.CinemaJwtAuthenticationConverter;
import com.cinema.common.security.web.reactive.CinemaServerAccessDeniedHandler;
import com.cinema.common.security.web.reactive.CinemaServerAuthenticationEntryPoint;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
public class GatewaySecurityConfiguration {

    private static final String[] PUBLIC_CATALOG_ENDPOINTS = {
        "/api/v1/movies",
        "/api/v1/movies/**",
        "/api/v1/genres",
        "/api/v1/genres/**",
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

    @Bean
    SecurityWebFilterChain gatewaySecurityWebFilterChain(
            ServerHttpSecurity http,
            CinemaJwtAuthenticationConverter jwtAuthenticationConverter,
            CinemaServerAuthenticationEntryPoint authenticationEntryPoint,
            CinemaServerAccessDeniedHandler accessDeniedHandler) {

        ReactiveJwtAuthenticationConverterAdapter reactiveJwtAuthenticationConverter =
                new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);

        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .requestCache(ServerHttpSecurity.RequestCacheSpec::disable)
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .authorizeExchange(
                        authorize ->
                                authorize
                                        .pathMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        .pathMatchers("/actuator/health", "/actuator/info")
                                        .permitAll()
                                        .pathMatchers(HttpMethod.GET, PUBLIC_CATALOG_ENDPOINTS)
                                        .permitAll()
                                        .pathMatchers("/api/**")
                                        .authenticated()
                                        .anyExchange()
                                        .denyAll())
                .exceptionHandling(
                        exceptionHandling ->
                                exceptionHandling
                                        .authenticationEntryPoint(authenticationEntryPoint)
                                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(
                        resourceServer ->
                                resourceServer
                                        .authenticationEntryPoint(authenticationEntryPoint)
                                        .accessDeniedHandler(accessDeniedHandler)
                                        .jwt(
                                                jwt ->
                                                        jwt.jwtAuthenticationConverter(
                                                                reactiveJwtAuthenticationConverter)))
                .build();
    }
}
