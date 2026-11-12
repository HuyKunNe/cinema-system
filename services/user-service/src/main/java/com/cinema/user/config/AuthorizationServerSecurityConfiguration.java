package com.cinema.user.config;

import com.cinema.common.security.jwt.AudienceValidator;
import com.cinema.common.security.jwt.CinemaJwtAuthenticationConverter;
import com.cinema.common.security.jwt.SubjectValidator;
import com.cinema.common.security.properties.SecurityProperties;
import com.cinema.common.security.web.CinemaAccessDeniedHandler;
import com.cinema.common.security.web.CinemaAuthenticationEntryPoint;
import com.cinema.user.oauth2.OidcLogoutRevocationSuccessHandler;
import com.cinema.user.security.GoogleLoginSuccessHandler;
import com.cinema.user.security.audit.impl.SecurityAuditAuthenticationFailureHandler;
import com.cinema.user.security.audit.impl.SecurityAuditAuthenticationSuccessHandler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Set;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AuthorizationServerSecurityConfiguration {

    private static final Set<String> APPROVED_GRANT_TYPES =
            Set.of("authorization_code", "refresh_token", "client_credentials");

    private static final String[] SWAGGER_ENDPOINTS = {
        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**"
    };

    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            SessionRegistry sessionRegistry,
            OidcLogoutRevocationSuccessHandler logoutSuccessHandler,
            AuthorizationServerSettings authorizationServerSettings)
            throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();

        http.setSharedObject(SessionRegistry.class, sessionRegistry);

        http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .cors(cors -> {})
                .with(
                        authorizationServerConfigurer,
                        authorizationServer ->
                                authorizationServer
                                        .oidc(oidc ->
                                                oidc.logoutEndpoint(logout ->
                                                        logout.logoutResponseHandler(
                                                                logoutSuccessHandler)))
                                        .authorizationServerMetadataEndpoint(metadata ->
                                                metadata.authorizationServerMetadataCustomizer(
                                                        builder ->
                                                                builder.grantTypes(grantTypes -> {
                                                                    grantTypes.clear();
                                                                    grantTypes.addAll(
                                                                            APPROVED_GRANT_TYPES);
                                                                }))))
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(
                                        authorizationServerSettings.getAuthorizationEndpoint())
                                .authenticated()
                                .anyRequest()
                                .permitAll())
                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(
                                new LoginUrlAuthenticationEntryPoint("/login")));

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain bearerUserApiSecurityFilterChain(
            HttpSecurity http,
            CinemaJwtAuthenticationConverter jwtAuthenticationConverter,
            SecurityProperties securityProperties,
            CinemaAuthenticationEntryPoint authenticationEntryPoint,
            CinemaAccessDeniedHandler accessDeniedHandler)
            throws Exception {

        JwtDecoder userApiJwtDecoder = userApiJwtDecoder(securityProperties);

        http.securityMatcher(bearerUserApiRequestMatcher())
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)
                .securityContext(context ->
                        context.securityContextRepository(new NullSecurityContextRepository()))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(HttpMethod.OPTIONS, "/api/v1/users/**")
                                .permitAll()
                                .requestMatchers("/api/v1/users/me", "/api/v1/users/me/**")
                                .authenticated()
                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/users/*/lock",
                                        "/api/v1/users/*/unlock",
                                        "/api/v1/users/*/disable",
                                        "/api/v1/users/*/enable")
                                .hasAuthority("user:manage")
                                .anyRequest()
                                .denyAll())
                .exceptionHandling(exceptions ->
                        exceptions
                                .authenticationEntryPoint(authenticationEntryPoint)
                                .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(resourceServer ->
                        resourceServer
                                .authenticationEntryPoint(authenticationEntryPoint)
                                .accessDeniedHandler(accessDeniedHandler)
                                .jwt(jwt ->
                                        jwt.decoder(userApiJwtDecoder)
                                                .jwtAuthenticationConverter(
                                                        jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    @Order(3)
    SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http,
            AuthenticationProvider userAuthenticationProvider,
            SessionRegistry sessionRegistry,
            SecurityAuditAuthenticationSuccessHandler authenticationSuccessHandler,
            SecurityAuditAuthenticationFailureHandler authenticationFailureHandler,
            GoogleLoginProperties googleLoginProperties,
            GoogleLoginSuccessHandler googleLoginSuccessHandler)
            throws Exception {

        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

        http.cors(cors -> {})
                .csrf(csrf ->
                        csrf.ignoringRequestMatchers(
                                "/api/v1/auth/registrations",
                                "/api/v1/auth/email-verifications/confirm"))
                .authenticationProvider(userAuthenticationProvider)
                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers("/login", "/css/**", "/favicon.ico")
                                .permitAll()
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/auth/registrations",
                                        "/api/v1/auth/email-verifications/confirm")
                                .permitAll()
                                .requestMatchers(SWAGGER_ENDPOINTS)
                                .permitAll()
                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/users/*/lock",
                                        "/api/v1/users/*/unlock",
                                        "/api/v1/users/*/disable",
                                        "/api/v1/users/*/enable")
                                .hasAuthority("user:manage")
                                .requestMatchers(
                                        "/actuator/health",
                                        "/actuator/info",
                                        "/error",
                                        "/.well-known/appspecific/**")
                                .permitAll()
                                .anyRequest()
                                .authenticated())
                .requestCache(cache -> cache.requestCache(requestCache))
                .sessionManagement(session ->
                        session.sessionFixation(fixation -> fixation.migrateSession())
                                .maximumSessions(-1)
                                .sessionRegistry(sessionRegistry))
                .formLogin(form ->
                        form.loginPage("/login")
                                .loginProcessingUrl("/login")
                                .successHandler(authenticationSuccessHandler)
                                .failureHandler(authenticationFailureHandler)
                                .permitAll());

        if (googleLoginProperties.enabled()) {
            http.oauth2Login(oauth2 ->
                    oauth2.loginPage("/login")
                            .successHandler(googleLoginSuccessHandler)
                            .failureUrl("/login?oauth2Error"));
        }

        return http.build();
    }

    private static RequestMatcher bearerUserApiRequestMatcher() {
        RequestMatcher userApiMatcher = new AntPathRequestMatcher("/api/v1/users/**");
        RequestMatcher bearerOrPreflightMatcher =
                request -> {
                    if (HttpMethod.OPTIONS.matches(request.getMethod())) {
                        return true;
                    }

                    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
                    if (authorization == null || authorization.length() < "Bearer".length()) {
                        return false;
                    }

                    return authorization.regionMatches(true, 0, "Bearer", 0, "Bearer".length())
                            && (authorization.length() == "Bearer".length()
                                    || Character.isWhitespace(
                                            authorization.charAt("Bearer".length())));
                };

        return new AndRequestMatcher(userApiMatcher, bearerOrPreflightMatcher);
    }

    private static JwtDecoder userApiJwtDecoder(SecurityProperties properties) {
        if (properties.issuerUri() == null || properties.issuerUri().isBlank()) {
            throw new IllegalStateException("OAuth2 issuer URI must be configured");
        }
        if (properties.jwkSetUri() == null || properties.jwkSetUri().isBlank()) {
            throw new IllegalStateException("OAuth2 JWK set URI must be configured");
        }

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri().trim()).build();
        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(properties.issuerUri().trim());
        OAuth2TokenValidator<Jwt> validators =
                new DelegatingOAuth2TokenValidator<>(
                        issuerValidator,
                        new AudienceValidator(properties.audience()),
                        new SubjectValidator());

        decoder.setJwtValidator(validators);
        return decoder;
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
