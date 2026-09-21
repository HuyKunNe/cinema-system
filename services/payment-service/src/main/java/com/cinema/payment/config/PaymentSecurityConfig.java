package com.cinema.payment.config;

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
public class PaymentSecurityConfig {

    private static final String PAYMENT_READ_AUTHORITY = "payment:read";

    private static final String PAYMENT_QUERY_ENDPOINT = "/api/v1/payments/*";

    private static final String PAYMENT_WEBHOOK_ENDPOINT = "/api/v1/payments/webhooks/*";

    private static final String PAYMENT_REFUND_AUTHORITY = "payment:refund";

    private static final String PAYMENT_RECONCILE_AUTHORITY = "payment:reconcile";

    private static final String PAYMENT_AUDIT_AUTHORITY = "payment:audit";

    private static final String[] SWAGGER_ENDPOINTS = {
        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**"
    };

    @Bean
    SecurityFilterChain paymentSecurityFilterChain(
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
                                        .requestMatchers(SWAGGER_ENDPOINTS)
                                        .permitAll()
                                        .requestMatchers("/actuator/health", "/actuator/info")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.POST, "/api/v1/payments/webhooks/*")
                                        .permitAll()
                                        .requestMatchers(
                                                HttpMethod.POST, "/api/v1/payments/*/refunds")
                                        .hasAuthority(PAYMENT_REFUND_AUTHORITY)
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/v1/payments/reconciliation-cases/*/resolve",
                                                "/api/v1/payments/reconciliation-cases/*/reject")
                                        .hasAuthority(PAYMENT_RECONCILE_AUTHORITY)
                                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/*/audit")
                                        .hasAuthority(PAYMENT_AUDIT_AUTHORITY)
                                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/*")
                                        .hasAuthority(PAYMENT_READ_AUTHORITY)
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
