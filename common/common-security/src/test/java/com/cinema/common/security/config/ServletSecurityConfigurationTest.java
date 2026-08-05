package com.cinema.common.security.config;

import com.cinema.common.jackson.config.JacksonConfiguration;
import com.cinema.common.security.web.CinemaAccessDeniedHandler;
import com.cinema.common.security.web.CinemaAuthenticationEntryPoint;
import com.cinema.common.security.web.SecurityResponseWriter;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ServletSecurityConfigurationTest {

    private final WebApplicationContextRunner webContextRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    ServletSecurityConfiguration.class))
                    .withBean(
                            ObjectMapper.class,
                            () -> new JacksonConfiguration()
                                    .objectMapper());

    private final ApplicationContextRunner nonWebContextRunner =
            new ApplicationContextRunner()
                    .withConfiguration(
                            AutoConfigurations.of(
                                    ServletSecurityConfiguration.class))
                    .withBean(
                            ObjectMapper.class,
                            () -> new JacksonConfiguration()
                                    .objectMapper());

    @Test
    void shouldCreateServletSecurityHandlersForWebApplication() {
        webContextRunner.run(context -> {
            assertThat(context)
                    .hasSingleBean(SecurityResponseWriter.class);

            assertThat(context)
                    .hasSingleBean(
                            CinemaAuthenticationEntryPoint.class);

            assertThat(context)
                    .hasSingleBean(
                            CinemaAccessDeniedHandler.class);
        });
    }

    @Test
    void shouldNotCreateServletHandlersForNonWebApplication() {
        nonWebContextRunner.run(context -> {
            assertThat(context)
                    .doesNotHaveBean(SecurityResponseWriter.class);

            assertThat(context)
                    .doesNotHaveBean(
                            CinemaAuthenticationEntryPoint.class);

            assertThat(context)
                    .doesNotHaveBean(
                            CinemaAccessDeniedHandler.class);
        });
    }
}
