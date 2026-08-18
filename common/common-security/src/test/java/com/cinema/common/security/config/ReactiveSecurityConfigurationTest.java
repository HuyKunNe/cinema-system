package com.cinema.common.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

class ReactiveSecurityConfigurationTest {

    private final ReactiveWebApplicationContextRunner contextRunner =
            new ReactiveWebApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    JacksonAutoConfiguration.class,
                                    SecurityConfiguration.class,
                                    ReactiveSecurityConfiguration.class));

    @Test
    void shouldConfigureReactiveDecoderFromSharedSecurityContract() {

        contextRunner
                .withPropertyValues(
                        "cinema.security.oauth2.issuer-uri=" + "https://identity.cinema.test",
                        "cinema.security.oauth2.jwk-set-uri="
                                + "https://identity.cinema.test/oauth2/jwks",
                        "cinema.security.oauth2.audience=" + "cinema-api")
                .run(context -> assertThat(context).hasSingleBean(ReactiveJwtDecoder.class));
    }

    @Test
    void shouldNotConfigureReactiveDecoderWhenContractIsIncomplete() {

        contextRunner
                .withPropertyValues(
                        "cinema.security.oauth2.issuer-uri=" + "https://identity.cinema.test",
                        "cinema.security.oauth2.audience=" + "cinema-api")
                .run(context -> assertThat(context).doesNotHaveBean(ReactiveJwtDecoder.class));
    }
}
