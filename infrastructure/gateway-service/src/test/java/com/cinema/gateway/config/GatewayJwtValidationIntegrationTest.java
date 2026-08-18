package com.cinema.gateway.config;

import com.cinema.gateway.GatewayServiceApplication;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterAll;
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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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
class GatewayJwtValidationIntegrationTest {

    private static final String ISSUER = "https://identity.cinema.test";

    private static final String AUDIENCE = "cinema-api";

    private static final String SUBJECT = "6f0da9a1-36bc-4c3a-b5e7-c26072924bb5";

    private static final RSAKey SIGNING_KEY = generateRsaKey("gateway-validation-key");

    private static final RSAKey UNTRUSTED_SIGNING_KEY = generateRsaKey("untrusted-validation-key");

    private static final HttpServer JWK_SERVER = startJwkServer();

    @Autowired private WebTestClient webTestClient;

    @DynamicPropertySource
    static void configureJwtValidation(DynamicPropertyRegistry registry) {

        registry.add("cinema.security.oauth2.issuer-uri", () -> ISSUER);

        registry.add(
                "cinema.security.oauth2.jwk-set-uri",
                GatewayJwtValidationIntegrationTest::jwkSetUri);

        registry.add("cinema.security.oauth2.audience", () -> AUDIENCE);
    }

    @AfterAll
    static void stopJwkServer() {

        JWK_SERVER.stop(0);
    }

    @Test
    void validJwtShouldPassGatewayAuthentication() {

        String token =
                token(
                        SIGNING_KEY,
                        claims ->
                                claims.claim("roles", List.of("USER"))
                                        .claim("permissions", List.of("BOOKING_CREATE")));

        webTestClient
                .post()
                .uri("/api/v1/bookings")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void jwtWithUntrustedSignatureShouldBeRejected() {

        String token = token(UNTRUSTED_SIGNING_KEY, claims -> {});

        assertUnauthorized(token);
    }

    @Test
    void jwtWithWrongIssuerShouldBeRejected() {

        String token = token(SIGNING_KEY, claims -> claims.issuer("https://untrusted.cinema.test"));

        assertUnauthorized(token);
    }

    @Test
    void jwtWithWrongAudienceShouldBeRejected() {

        String token = token(SIGNING_KEY, claims -> claims.audience("untrusted-api"));

        assertUnauthorized(token);
    }

    @Test
    void expiredJwtShouldBeRejected() {

        Instant now = Instant.now();

        String token =
                token(
                        SIGNING_KEY,
                        claims ->
                                claims.issueTime(Date.from(now.minusSeconds(600)))
                                        .notBeforeTime(Date.from(now.minusSeconds(600)))
                                        .expirationTime(Date.from(now.minusSeconds(300))));

        assertUnauthorized(token);
    }

    @Test
    void jwtBeforeNotBeforeTimeShouldBeRejected() {

        Instant now = Instant.now();

        String token =
                token(
                        SIGNING_KEY,
                        claims ->
                                claims.notBeforeTime(Date.from(now.plusSeconds(300)))
                                        .expirationTime(Date.from(now.plusSeconds(600))));

        assertUnauthorized(token);
    }

    private void assertUnauthorized(String token) {

        webTestClient
                .post()
                .uri("/api/v1/bookings")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
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

    private static String token(
            RSAKey signingKey, Consumer<JWTClaimsSet.Builder> claimsCustomizer) {

        Instant now = Instant.now();

        JWTClaimsSet.Builder claims =
                new JWTClaimsSet.Builder()
                        .jwtID(UUID.randomUUID().toString())
                        .subject(SUBJECT)
                        .issuer(ISSUER)
                        .audience(AUDIENCE)
                        .issueTime(Date.from(now.minusSeconds(5)))
                        .notBeforeTime(Date.from(now.minusSeconds(5)))
                        .expirationTime(Date.from(now.plusSeconds(300)));

        claimsCustomizer.accept(claims);

        JWSHeader header =
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .type(JOSEObjectType.JWT)
                        .keyID(signingKey.getKeyID())
                        .build();

        SignedJWT signedJwt = new SignedJWT(header, claims.build());

        try {
            signedJwt.sign(new RSASSASigner(signingKey));
        } catch (JOSEException exception) {
            throw new IllegalStateException("Could not sign test JWT", exception);
        }

        return signedJwt.serialize();
    }

    private static RSAKey generateRsaKey(String keyId) {

        try {
            return new RSAKeyGenerator(2048).keyID(keyId).generate();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Could not generate test RSA key", exception);
        }
    }

    private static HttpServer startJwkServer() {

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

            server.createContext(
                    "/oauth2/jwks",
                    exchange -> {
                        byte[] response =
                                new JWKSet(SIGNING_KEY.toPublicJWK())
                                        .toString()
                                        .getBytes(StandardCharsets.UTF_8);

                        exchange.getResponseHeaders()
                                .set(HttpHeaders.CONTENT_TYPE, "application/json");

                        exchange.sendResponseHeaders(200, response.length);

                        try (OutputStream output = exchange.getResponseBody()) {

                            output.write(response);
                        }
                    });

            server.start();

            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start test JWK server", exception);
        }
    }

    private static String jwkSetUri() {

        return "http://localhost:" + JWK_SERVER.getAddress().getPort() + "/oauth2/jwks";
    }

    private static String bearer(String token) {

        return "Bearer " + token;
    }
}
