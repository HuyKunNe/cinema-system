package com.cinema.common.security.config;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.security.error.SecurityErrorCode;
import com.cinema.common.security.jwt.AudienceValidator;
import com.cinema.common.security.properties.SecurityProperties;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@AutoConfiguration(after = SecurityConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class ReactiveSecurityConfiguration {

    @Bean
    @ConditionalOnMissingBean(ReactiveJwtDecoder.class)
    @ConditionalOnProperty(
            prefix = "cinema.security.oauth2",
            name = {"issuer-uri", "jwk-set-uri", "audience"})
    public ReactiveJwtDecoder cinemaReactiveJwtDecoder(
            SecurityProperties properties, AudienceValidator audienceValidator) {

        validateIssuer(properties.issuerUri());
        validateJwkSetUri(properties.jwkSetUri());

        NimbusReactiveJwtDecoder decoder =
                NimbusReactiveJwtDecoder.withJwkSetUri(properties.jwkSetUri().trim()).build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(properties.issuerUri().trim());

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));

        return decoder;
    }

    private static void validateIssuer(String issuerUri) {

        if (issuerUri == null || issuerUri.isBlank()) {

            throw new InternalServerException(SecurityErrorCode.INVALID_ISSUER_CONFIGURATION);
        }
    }

    private static void validateJwkSetUri(String jwkSetUri) {

        if (jwkSetUri == null || jwkSetUri.isBlank()) {

            throw new InternalServerException(SecurityErrorCode.INVALID_JWK_SET_CONFIGURATION);
        }
    }
}
