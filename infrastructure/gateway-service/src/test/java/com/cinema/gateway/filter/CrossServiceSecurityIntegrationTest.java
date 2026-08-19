package com.cinema.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinema.common.test.security.TestJwtIssuer;
import com.cinema.gateway.GatewayServiceApplication;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
class CrossServiceSecurityIntegrationTest {

    private static final String SERVICE_CLIENT_ID = "booking-service";

    private static final TestJwtIssuer JWT_ISSUER =
            new TestJwtIssuer("https://identity.cross-service.test", "cinema-api");

    private static final AtomicReference<HttpHeaders> FORWARDED_HEADERS = new AtomicReference<>();

    private static final AtomicInteger DOWNSTREAM_REQUEST_COUNT = new AtomicInteger();

    private static final HttpServer DOWNSTREAM_SERVER = startDownstreamServer();

    @Autowired private WebTestClient webTestClient;

    @DynamicPropertySource
    static void configureSecurityAndRoute(DynamicPropertyRegistry registry) {

        registry.add("cinema.security.oauth2.issuer-uri", JWT_ISSUER::issuer);

        registry.add("cinema.security.oauth2.jwk-set-uri", JWT_ISSUER::jwkSetUri);

        registry.add("cinema.security.oauth2.audience", JWT_ISSUER::audience);

        registry.add(
                "spring.cloud.gateway.server.webflux" + ".routes[0].id",
                () -> "inventory-service-test");

        registry.add(
                "spring.cloud.gateway.server.webflux" + ".routes[0].uri",
                CrossServiceSecurityIntegrationTest::downstreamUri);

        registry.add(
                "spring.cloud.gateway.server.webflux" + ".routes[0].predicates[0]",
                () -> "Path=/api/v1/show-seats/**");
    }

    @BeforeEach
    void resetDownstreamCapture() {

        FORWARDED_HEADERS.set(null);
        DOWNSTREAM_REQUEST_COUNT.set(0);
    }

    @AfterAll
    static void stopServers() {

        JWT_ISSUER.close();
        DOWNSTREAM_SERVER.stop(0);
    }

    @Test
    void validServiceTokenShouldBeForwardedToDownstream() {

        String token = serviceToken();

        webTestClient
                .put()
                .uri("/api/v1/show-seats/" + "019c1234-1111-7abc-8def-0123456789ab" + "/book")
                .header(HttpHeaders.AUTHORIZATION, JWT_ISSUER.bearer(token))
                .bodyValue(
                        """
                        {
                          "bookingId":
                            "019c1234-2222-7abc-8def-0123456789ab"
                        }
                        """)
                .exchange()
                .expectStatus()
                .isNoContent();

        assertThat(DOWNSTREAM_REQUEST_COUNT.get()).isEqualTo(1);

        assertThat(FORWARDED_HEADERS.get()).isNotNull();

        assertThat(FORWARDED_HEADERS.get().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo(JWT_ISSUER.bearer(token));
    }

    @Test
    void gatewayShouldRemoveClientSuppliedIdentityHeaders() {

        String token = serviceToken();

        webTestClient
                .put()
                .uri("/api/v1/show-seats/" + "019c1234-1111-7abc-8def-0123456789ab" + "/book")
                .header(HttpHeaders.AUTHORIZATION, JWT_ISSUER.bearer(token))
                .header("X-User-Id", "019c1234-3333-7abc-8def-0123456789ab")
                .header("X-Roles", "ADMIN,SERVICE")
                .header("X-Permissions", "inventory:manage,user:manage")
                .header("X-Client-Id", "spoofed-service")
                .bodyValue(
                        """
                        {
                          "bookingId":
                            "019c1234-2222-7abc-8def-0123456789ab"
                        }
                        """)
                .exchange()
                .expectStatus()
                .isNoContent();

        HttpHeaders forwardedHeaders = FORWARDED_HEADERS.get();

        assertThat(forwardedHeaders).isNotNull();

        assertThat(forwardedHeaders.getFirst("X-User-Id")).isNull();

        assertThat(forwardedHeaders.getFirst("X-Roles")).isNull();

        assertThat(forwardedHeaders.getFirst("X-Permissions")).isNull();

        assertThat(forwardedHeaders.getFirst("X-Client-Id")).isNull();

        assertThat(forwardedHeaders.getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo(JWT_ISSUER.bearer(token));
    }

    @Test
    void invalidServiceTokenShouldNotReachDownstream() {

        String token =
                JWT_ISSUER.untrustedToken(
                        claims ->
                                claims.subject(SERVICE_CLIENT_ID)
                                        .claim("permissions", List.of("inventory:write")));

        webTestClient
                .put()
                .uri("/api/v1/show-seats/" + "019c1234-1111-7abc-8def-0123456789ab" + "/book")
                .header(HttpHeaders.AUTHORIZATION, JWT_ISSUER.bearer(token))
                .bodyValue(
                        """
                        {
                          "bookingId":
                            "019c1234-2222-7abc-8def-0123456789ab"
                        }
                        """)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.success")
                .isEqualTo(false)
                .jsonPath("$.error.code")
                .isEqualTo("SECURITY_AUTHENTICATION_REQUIRED");

        assertThat(DOWNSTREAM_REQUEST_COUNT.get()).isZero();

        assertThat(FORWARDED_HEADERS.get()).isNull();
    }

    @Test
    void spoofedHeadersWithoutBearerShouldNotReachDownstream() {

        webTestClient
                .put()
                .uri("/api/v1/show-seats/" + "019c1234-1111-7abc-8def-0123456789ab" + "/book")
                .header("X-Roles", "SERVICE")
                .header("X-Permissions", "inventory:write")
                .header("X-Client-Id", SERVICE_CLIENT_ID)
                .bodyValue(
                        """
                        {
                          "bookingId":
                            "019c1234-2222-7abc-8def-0123456789ab"
                        }
                        """)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.error.code")
                .isEqualTo("SECURITY_AUTHENTICATION_REQUIRED");

        assertThat(DOWNSTREAM_REQUEST_COUNT.get()).isZero();
    }

    private static String serviceToken() {

        return JWT_ISSUER.token(
                claims ->
                        claims.subject(SERVICE_CLIENT_ID)
                                .claim("permissions", List.of("inventory:write")));
    }

    private static HttpServer startDownstreamServer() {

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

            server.createContext(
                    "/",
                    exchange -> {
                        DOWNSTREAM_REQUEST_COUNT.incrementAndGet();

                        HttpHeaders capturedHeaders = new HttpHeaders();

                        exchange.getRequestHeaders().forEach(capturedHeaders::put);

                        FORWARDED_HEADERS.set(HttpHeaders.readOnlyHttpHeaders(capturedHeaders));

                        exchange.getRequestBody().readAllBytes();

                        exchange.sendResponseHeaders(204, -1);

                        exchange.close();
                    });

            server.start();

            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start downstream test server", exception);
        }
    }

    private static String downstreamUri() {

        return "http://localhost:" + DOWNSTREAM_SERVER.getAddress().getPort();
    }
}
