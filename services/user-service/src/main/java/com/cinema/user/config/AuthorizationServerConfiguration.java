package com.cinema.user.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;

import com.cinema.user.oauth2.token.RefreshTokenTrackingService;
import com.cinema.user.oauth2.token.TrackingOAuth2AuthorizationService;
import com.cinema.user.security.CinemaUserDetails;
import com.cinema.user.security.CinemaUserDetailsMixin;
import com.cinema.user.security.jwt.JwtSigningKeyLoader;
import com.cinema.user.security.jwt.RsaSigningKeyPair;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        AuthorizationServerProperties.class,
        JwtSigningKeyProperties.class,
        JwtClaimsProperties.class
})
public class AuthorizationServerConfiguration {

    @Bean
    AuthorizationServerSettings authorizationServerSettings(
            AuthorizationServerProperties properties) {

        return AuthorizationServerSettings.builder()
                .issuer(properties.issuer())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "cinema.user.authorization-server.signing", name = "enabled", havingValue = "true")
    JWKSource<SecurityContext> jwkSource(
            JwtSigningKeyProperties properties,
            JwtSigningKeyLoader keyLoader) {

        RsaSigningKeyPair keyPair = keyLoader.load(properties);

        RSAKey rsaKey = new RSAKey.Builder(keyPair.publicKey())
                .privateKey(keyPair.privateKey())
                .keyID(properties.keyId())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .build();

        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    @ConditionalOnProperty(prefix = "cinema.user.authorization-server.signing", name = "enabled", havingValue = "true")
    JwtEncoder jwtEncoder(
            JWKSource<SecurityContext> jwkSource) {

        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    @ConditionalOnProperty(prefix = "cinema.user.authorization-server.signing", name = "enabled", havingValue = "true")
    JwtDecoder jwtDecoder(
            JWKSource<SecurityContext> jwkSource) {

        return org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
                .jwtDecoder(jwkSource);
    }

    @Bean
    OAuth2AuthorizationService authorizationService(
            JdbcOperations jdbcOperations,
            RegisteredClientRepository registeredClientRepository,
            RefreshTokenTrackingService refreshTokenTrackingService) {

        JdbcOAuth2AuthorizationService authorizationService = new JdbcOAuth2AuthorizationService(
                jdbcOperations,
                registeredClientRepository);

        ObjectMapper objectMapper = oauth2AuthorizationObjectMapper();

        JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper rowMapper = new JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper(
                registeredClientRepository);
        rowMapper.setObjectMapper(objectMapper);
        authorizationService.setAuthorizationRowMapper(rowMapper);

        JdbcOAuth2AuthorizationService.OAuth2AuthorizationParametersMapper parametersMapper = new JdbcOAuth2AuthorizationService.OAuth2AuthorizationParametersMapper();
        parametersMapper.setObjectMapper(objectMapper);
        authorizationService.setAuthorizationParametersMapper(parametersMapper);

        return new TrackingOAuth2AuthorizationService(
                authorizationService,
                refreshTokenTrackingService);
    }

    @Bean
    OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcOperations jdbcOperations,
            RegisteredClientRepository registeredClientRepository) {

        return new JdbcOAuth2AuthorizationConsentService(
                jdbcOperations,
                registeredClientRepository);
    }

    private ObjectMapper oauth2AuthorizationObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModules(SecurityJackson2Modules.getModules(
                AuthorizationServerConfiguration.class.getClassLoader()));
        objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
        objectMapper.addMixIn(CinemaUserDetails.class, CinemaUserDetailsMixin.class);
        return objectMapper;
    }
}
