package com.cinema.common.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.security.jwt.AudienceValidator;
import com.cinema.common.security.jwt.CinemaJwtAuthenticationConverter;
import com.cinema.common.security.jwt.CinemaJwtGrantedAuthoritiesConverter;
import com.cinema.common.security.jwt.SubjectValidator;
import com.cinema.common.security.properties.SecurityProperties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class SecurityConfigurationTest {

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(SecurityConfiguration.class));

    @Test
    void shouldConfigureSharedSecurityBeans() {
        contextRunner
                .withPropertyValues(
                        "cinema.security.oauth2.issuer-uri=" + "https://identity.cinema.test",
                        "cinema.security.oauth2.audience=" + "cinema-api",
                        "cinema.security.oauth2.jwk-set-uri="
                                + "https://identity.cinema.test/oauth2/jwks")
                .run(
                        context -> {
                            assertThat(context).hasSingleBean(SecurityProperties.class);

                            assertThat(context).hasSingleBean(AudienceValidator.class);

                            assertThat(context)
                                    .hasSingleBean(CinemaJwtGrantedAuthoritiesConverter.class);

                            assertThat(context)
                                    .hasSingleBean(CinemaJwtAuthenticationConverter.class);

                            assertThat(context).hasSingleBean(JwtDecoder.class);

                            SecurityProperties properties =
                                    context.getBean(SecurityProperties.class);

                            assertThat(properties.issuerUri())
                                    .isEqualTo("https://identity.cinema.test");

                            assertThat(properties.audience()).isEqualTo("cinema-api");

                            assertThat(properties.jwkSetUri())
                                    .isEqualTo("https://identity.cinema.test/oauth2/jwks");

                            assertThat(context).hasSingleBean(SubjectValidator.class);
                        });
    }

    @Test
    void shouldNotCreateAudienceValidatorWithoutAudienceProperty() {
        contextRunner.run(
                context -> {
                    assertThat(context).doesNotHaveBean(AudienceValidator.class);

                    assertThat(context).hasSingleBean(CinemaJwtAuthenticationConverter.class);

                    assertThat(context).doesNotHaveBean(JwtDecoder.class);

                    assertThat(context).hasSingleBean(SubjectValidator.class);
                });
    }
}
