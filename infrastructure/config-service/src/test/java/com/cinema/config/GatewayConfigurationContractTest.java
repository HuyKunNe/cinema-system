package com.cinema.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

class GatewayConfigurationContractTest {

    private PropertySource<?> gatewayConfiguration;

    @BeforeEach
    void loadGatewayConfiguration() throws IOException {

        ClassPathResource resource = new ClassPathResource("config-repo/gateway-service.yml");

        List<PropertySource<?>> propertySources =
                new YamlPropertySourceLoader().load("gateway-service", resource);

        assertThat(propertySources).hasSize(1);

        gatewayConfiguration = propertySources.getFirst();
    }

    @Test
    void discoveryLocatorShouldBeDisabled() {

        assertProperty("spring.cloud.gateway.server.webflux.discovery.locator.enabled", false);
    }

    @Test
    void shouldDeclareOnlyExplicitServiceRoutes() {

        assertRoute(
                0,
                "movie-service",
                "lb://movie-service",
                "Path=/api/v1/movies/**,/api/v1/genres/**");

        assertRoute(1, "user-service", "lb://user-service", "Path=/api/v1/users/**");

        assertRoute(
                2,
                "inventory-service",
                "lb://inventory-service",
                "Path=/api/v1/cinemas/**,/api/v1/rooms/**,/api/v1/showtimes/**,/api/v1/seats/**,/api/v1/show-seats/**");

        assertRoute(3, "booking-service", "lb://booking-service", "Path=/api/v1/bookings/**");

        assertRoute(4, "payment-service", "lb://payment-service", "Path=/api/v1/payments/**");

        assertRoute(
                5,
                "notification-service",
                "lb://notification-service",
                "Path=/api/v1/notifications/**");

        assertThat(
                        gatewayConfiguration.getProperty(
                                "spring.cloud.gateway.server.webflux.routes[6].id"))
                .isNull();
    }

    @Test
    void shouldUseExternalizedOAuth2TrustConfiguration() {

        assertProperty("cinema.security.oauth2.issuer-uri", "${CINEMA_AUTH_ISSUER}");

        assertProperty("cinema.security.oauth2.jwk-set-uri", "${CINEMA_AUTH_JWK_SET_URI}");

        assertProperty("cinema.security.oauth2.audience", "${CINEMA_AUTH_AUDIENCE:cinema-api}");
    }

    @Test
    void shouldForwardAuthorizationAndTracingHeadersThroughCors() {

        assertProperty(
                "spring.cloud.gateway.server.webflux.globalcors"
                        + ".cors-configurations[/**].allowed-headers[0]",
                "Authorization");

        assertThat(corsAllowedHeaders())
                .contains("Authorization", "Content-Type", "X-Request-Id", "X-Correlation-Id");
    }

    private void assertRoute(
            int index, String expectedId, String expectedUri, String expectedPathPredicate) {

        String routePrefix = "spring.cloud.gateway.server.webflux.routes[" + index + "]";

        assertProperty(routePrefix + ".id", expectedId);

        assertProperty(routePrefix + ".uri", expectedUri);

        assertProperty(routePrefix + ".predicates[0]", expectedPathPredicate);
    }

    private List<Object> corsAllowedHeaders() {

        String propertyPrefix =
                "spring.cloud.gateway.server.webflux.globalcors"
                        + ".cors-configurations[/**].allowed-headers";

        return List.of(
                property(propertyPrefix + "[0]"),
                property(propertyPrefix + "[1]"),
                property(propertyPrefix + "[2]"),
                property(propertyPrefix + "[3]"),
                property(propertyPrefix + "[4]"),
                property(propertyPrefix + "[5]"),
                property(propertyPrefix + "[6]"));
    }

    private void assertProperty(String propertyName, Object expectedValue) {

        assertThat(property(propertyName)).isEqualTo(expectedValue);
    }

    private Object property(String propertyName) {

        return gatewayConfiguration.getProperty(propertyName);
    }
}
