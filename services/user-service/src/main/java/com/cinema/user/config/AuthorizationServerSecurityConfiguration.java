package com.cinema.user.config;

import com.cinema.user.oauth2.OidcLogoutRevocationSuccessHandler;
import com.cinema.user.security.audit.impl.SecurityAuditAuthenticationFailureHandler;
import com.cinema.user.security.audit.impl.SecurityAuditAuthenticationSuccessHandler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import java.util.Set;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AuthorizationServerSecurityConfiguration {

    private static final Set<String> APPROVED_GRANT_TYPES =
            Set.of("authorization_code", "refresh_token", "client_credentials");

    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            SessionRegistry sessionRegistry,
            OidcLogoutRevocationSuccessHandler logoutSuccessHandler)
            throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();

        http.setSharedObject(SessionRegistry.class, sessionRegistry);

        http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(
                        authorizationServerConfigurer,
                        authorizationServer ->
                                authorizationServer
                                        .oidc(
                                                oidc ->
                                                        oidc.logoutEndpoint(
                                                                logout ->
                                                                        logout
                                                                                .logoutResponseHandler(
                                                                                        logoutSuccessHandler)))
                                        .authorizationServerMetadataEndpoint(
                                                metadata ->
                                                        metadata
                                                                .authorizationServerMetadataCustomizer(
                                                                        builder ->
                                                                                builder.grantTypes(
                                                                                        grantTypes -> {
                                                                                            grantTypes
                                                                                                    .clear();
                                                                                            grantTypes
                                                                                                    .addAll(
                                                                                                            APPROVED_GRANT_TYPES);
                                                                                        }))))
                .exceptionHandling(
                        exceptions ->
                                exceptions.authenticationEntryPoint(
                                        new LoginUrlAuthenticationEntryPoint("/login")));

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            AuthenticationProvider userAuthenticationProvider,
            SessionRegistry sessionRegistry,
            SecurityAuditAuthenticationSuccessHandler authenticationSuccessHandler,
            SecurityAuditAuthenticationFailureHandler authenticationFailureHandler)
            throws Exception {

        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

        http.authenticationProvider(userAuthenticationProvider)
                .authorizeHttpRequests(
                        authorize ->
                                authorize
                                        .requestMatchers(
                                                "/actuator/health", "/actuator/info", "/error")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .requestCache(cache -> cache.requestCache(requestCache))
                .sessionManagement(
                        session ->
                                session.sessionFixation(fixation -> fixation.migrateSession())
                                        .maximumSessions(-1)
                                        .sessionRegistry(sessionRegistry))
                .formLogin(
                        form ->
                                form.successHandler(authenticationSuccessHandler)
                                        .failureHandler(authenticationFailureHandler));

        return http.build();
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    static HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
