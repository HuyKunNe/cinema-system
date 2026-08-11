package com.cinema.user.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;

class JwtClaimsPropertiesTest {

    private static final String PREFIX = "cinema.user.authorization-server.jwt";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldBindSingleAudience() {
        contextRunner
                .withPropertyValues(PREFIX + ".audiences=cinema-api")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context)
                            .hasSingleBean(JwtClaimsProperties.class);
                    assertThat(context.getBean(JwtClaimsProperties.class)
                            .audiences())
                            .containsExactly("cinema-api");
                });
    }

    @Test
    void shouldBindMultipleAudiences() {
        contextRunner
                .withPropertyValues(
                        PREFIX + ".audiences=cinema-api,inventory-service")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(JwtClaimsProperties.class)
                            .audiences())
                            .containsExactly(
                                    "cinema-api",
                                    "inventory-service");
                });
    }

    @Test
    void shouldRejectMissingAudience() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();

            assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(BindValidationException.class)
                    .hasMessageContaining("audiences")
                    .hasMessageContaining("must not be empty");
        });
    }

    @Test
    void shouldRejectBlankAudience() {
        contextRunner
                .withPropertyValues(
                        PREFIX + ".audiences[0]=cinema-api",
                        PREFIX + ".audiences[1]=")
                .run(context -> {
                    assertThat(context).hasFailed();

                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .isInstanceOf(BindValidationException.class)
                            .hasMessageContaining("audiences[1]")
                            .hasMessageContaining("must not be blank");
                });
    }

    @Test
    void shouldExposeImmutableAudienceList() {
        contextRunner
                .withPropertyValues(PREFIX + ".audiences=cinema-api")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    JwtClaimsProperties properties = context.getBean(JwtClaimsProperties.class);

                    assertThatThrownBy(() -> properties.audiences()
                            .add("payment-service"))
                            .isInstanceOf(UnsupportedOperationException.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtClaimsProperties.class)
    static class TestConfiguration {
    }
}
