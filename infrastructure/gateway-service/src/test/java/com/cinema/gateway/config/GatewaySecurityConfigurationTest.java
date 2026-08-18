package com.cinema.gateway.config;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

import com.cinema.gateway.GatewayServiceApplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

@ActiveProfiles("test")
@AutoConfigureWebTestClient
@SpringBootTest(
        classes = GatewayServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "spring.cloud.config.enabled=false",
            "spring.config.import=",
            "spring.cloud.discovery.enabled=false",
            "eureka.client.enabled=false"
        })
class GatewaySecurityConfigurationTest {

    @Autowired private WebTestClient webTestClient;

    @Test
    void healthEndpointShouldBePublic() {

        webTestClient.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }

    @Test
    void publicCatalogEndpointShouldNotRequireAuthentication() {

        webTestClient.get().uri("/api/v1/movies").exchange().expectStatus().isNotFound();
    }

    @Test
    void protectedApiShouldRejectAnonymousRequest() {

        webTestClient
                .post()
                .uri("/api/v1/bookings")
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectHeader()
                .contentType("application/json")
                .expectBody()
                .jsonPath("$.success")
                .isEqualTo(false)
                .jsonPath("$.error.code")
                .isEqualTo("SECURITY_AUTHENTICATION_REQUIRED")
                .jsonPath("$.error.category")
                .isEqualTo("SECURITY");
    }

    @Test
    void authenticatedRequestShouldPassGatewayAuthentication() {

        webTestClient
                .mutateWith(
                        mockJwt()
                                .jwt(
                                        jwt ->
                                                jwt.subject("6f0da9a1-36bc-4c3a-b5e7-c26072924bb5")
                                                        .claim("roles", List.of("USER"))
                                                        .claim(
                                                                "permissions",
                                                                List.of("BOOKING_CREATE"))))
                .post()
                .uri("/api/v1/bookings")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void undeclaredEndpointShouldBeForbiddenForAuthenticatedUser() {

        webTestClient
                .mutateWith(
                        mockJwt().jwt(jwt -> jwt.subject("6f0da9a1-36bc-4c3a-b5e7-c26072924bb5")))
                .get()
                .uri("/internal/not-exposed")
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectHeader()
                .contentType("application/json")
                .expectBody()
                .jsonPath("$.success")
                .isEqualTo(false)
                .jsonPath("$.error.code")
                .isEqualTo("SECURITY_ACCESS_DENIED")
                .jsonPath("$.error.category")
                .isEqualTo("SECURITY");
    }

    @Test
    void corsPreflightShouldBePermitted() {

        webTestClient.options().uri("/api/v1/bookings").exchange().expectStatus().isNotFound();
    }
}
