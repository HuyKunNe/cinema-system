package com.cinema.common.test.security;

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

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;

public final class TestJwtIssuer implements AutoCloseable {

    private static final String JWK_PATH = "/oauth2/jwks";

    private final String issuer;

    private final String audience;

    private final RSAKey trustedSigningKey;

    private final RSAKey untrustedSigningKey;

    private final HttpServer jwkServer;

    public TestJwtIssuer(String issuer, String audience) {

        this.issuer = requireText(issuer, "issuer");

        this.audience = requireText(audience, "audience");

        this.trustedSigningKey = generateRsaKey("trusted-test-key");

        this.untrustedSigningKey = generateRsaKey("untrusted-test-key");

        this.jwkServer = startJwkServer();
    }

    public String issuer() {

        return issuer;
    }

    public String audience() {

        return audience;
    }

    public String jwkSetUri() {

        return "http://localhost:" + jwkServer.getAddress().getPort() + JWK_PATH;
    }

    public String token(Consumer<JWTClaimsSet.Builder> claimsCustomizer) {

        return token(trustedSigningKey, claimsCustomizer);
    }

    public String untrustedToken(Consumer<JWTClaimsSet.Builder> claimsCustomizer) {

        return token(untrustedSigningKey, claimsCustomizer);
    }

    public String bearer(String token) {

        return "Bearer " + token;
    }

    @Override
    public void close() {

        jwkServer.stop(0);
    }

    private String token(RSAKey signingKey, Consumer<JWTClaimsSet.Builder> claimsCustomizer) {

        Instant now = Instant.now();

        JWTClaimsSet.Builder claims =
                new JWTClaimsSet.Builder()
                        .jwtID(UUID.randomUUID().toString())
                        .subject(UUID.randomUUID().toString())
                        .issuer(issuer)
                        .audience(audience)
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

    private HttpServer startJwkServer() {

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

            server.createContext(
                    JWK_PATH,
                    exchange -> {
                        byte[] response =
                                new JWKSet(trustedSigningKey.toPublicJWK())
                                        .toString()
                                        .getBytes(StandardCharsets.UTF_8);

                        exchange.getResponseHeaders().set("Content-Type", "application/json");

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

    private static RSAKey generateRsaKey(String keyId) {

        try {
            return new RSAKeyGenerator(2048).keyID(keyId).generate();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Could not generate test RSA key", exception);
        }
    }

    private static String requireText(String value, String fieldName) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return value;
    }
}
