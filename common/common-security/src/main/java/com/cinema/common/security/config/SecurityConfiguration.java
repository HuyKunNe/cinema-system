package com.cinema.common.security.config;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.common.security.error.SecurityErrorCode;
import com.cinema.common.security.jwt.AudienceValidator;
import com.cinema.common.security.jwt.CinemaJwtAuthenticationConverter;
import com.cinema.common.security.jwt.CinemaJwtGrantedAuthoritiesConverter;
import com.cinema.common.security.jwt.SubjectValidator;
import com.cinema.common.security.properties.SecurityProperties;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@AutoConfiguration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CinemaJwtGrantedAuthoritiesConverter cinemaJwtGrantedAuthoritiesConverter() {

        return new CinemaJwtGrantedAuthoritiesConverter();
    }

    @Bean
    @ConditionalOnMissingBean
    public CinemaJwtAuthenticationConverter cinemaJwtAuthenticationConverter(
            CinemaJwtGrantedAuthoritiesConverter authoritiesConverter) {

        return new CinemaJwtAuthenticationConverter(authoritiesConverter);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "cinema.security.oauth2", name = "audience")
    public AudienceValidator audienceValidator(SecurityProperties properties) {

        return new AudienceValidator(properties.audience());
    }

    @Bean
    @ConditionalOnMissingBean
    public SubjectValidator subjectValidator() {

        return new SubjectValidator();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnMissingBean(JwtDecoder.class)
    @ConditionalOnProperty(
            prefix = "cinema.security.oauth2",
            name = {"issuer-uri", "jwk-set-uri", "audience"})
    public JwtDecoder cinemaJwtDecoder(
            SecurityProperties properties,
            AudienceValidator audienceValidator,
            SubjectValidator subjectValidator) {

        validateIssuer(properties.issuerUri());
        validateJwkSetUri(properties.jwkSetUri());

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri().trim()).build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(properties.issuerUri().trim());

        OAuth2TokenValidator<Jwt> validator =
                new DelegatingOAuth2TokenValidator<>(
                        issuerValidator, audienceValidator, subjectValidator);

        decoder.setJwtValidator(validator);

        return decoder;
    }

    private void validateIssuer(String issuerUri) {
        if (issuerUri == null || issuerUri.isBlank()) {
            throw new InternalServerException(SecurityErrorCode.INVALID_ISSUER_CONFIGURATION);
        }
    }

    private void validateJwkSetUri(String jwkSetUri) {
        if (jwkSetUri == null || jwkSetUri.isBlank()) {
            throw new InternalServerException(SecurityErrorCode.INVALID_JWK_SET_CONFIGURATION);
        }
    }
}
