package com.cinema.user.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.cinema.common.test.container.AbstractMySqlIntegrationTest;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@SpringBootTest(properties = {
        "spring.main.web-application-type=servlet",
        "cinema.user.authorization-server.issuer=http://localhost:8082",
        "cinema.user.authorization-server.jwt.audiences=cinema-api"
})
@AutoConfigureMockMvc
class JwkSetEndpointIntegrationTest
        extends AbstractMySqlIntegrationTest {

    private static final String ISSUER = "http://localhost:8082";
    private static final String KEY_ID = "cinema-user-integration-test-key";

    private static final TestRsaKeyMaterial KEY_MATERIAL = TestRsaKeyMaterial.generate();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @DynamicPropertySource
    static void configureSigningProperties(
            DynamicPropertyRegistry registry) {

        registry.add(
                "cinema.user.authorization-server.signing.enabled",
                () -> "true");
        registry.add(
                "cinema.user.authorization-server.signing.key-id",
                () -> KEY_ID);
        registry.add(
                "cinema.user.authorization-server.signing.private-key-location",
                KEY_MATERIAL::privateKeyLocation);
        registry.add(
                "cinema.user.authorization-server.signing.public-key-location",
                KEY_MATERIAL::publicKeyLocation);
    }

    @Test
    void jwkSetEndpointShouldPublishApprovedPublicRsaKey()
            throws Exception {

        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys.length()").value(1))
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].kid").value(KEY_ID))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].n").isNotEmpty())
                .andExpect(jsonPath("$.keys[0].e").isNotEmpty());
    }

    @Test
    void jwkSetEndpointShouldNotExposePrivateRsaParameters()
            throws Exception {

        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].d").doesNotExist())
                .andExpect(jsonPath("$.keys[0].p").doesNotExist())
                .andExpect(jsonPath("$.keys[0].q").doesNotExist())
                .andExpect(jsonPath("$.keys[0].dp").doesNotExist())
                .andExpect(jsonPath("$.keys[0].dq").doesNotExist())
                .andExpect(jsonPath("$.keys[0].qi").doesNotExist())
                .andExpect(jsonPath("$.keys[0].oth").doesNotExist());
    }

    @Test
    void authorizationServerMetadataShouldPublishJwkSetUri()
            throws Exception {

        mockMvc.perform(get(
                "/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value(ISSUER))
                .andExpect(jsonPath("$.jwks_uri")
                        .value(ISSUER + "/oauth2/jwks"));
    }

    @Test
    void publicKeyFromJwkEndpointShouldVerifySignedJwt()
            throws Exception {

        MvcResult result = mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andReturn();

        String jwkSetJson = result.getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        JWKSet publicJwkSet = JWKSet.parse(jwkSetJson);
        JWKSource<SecurityContext> publicJwkSource = new ImmutableJWKSet<>(publicJwkSet);

        JwtDecoder publicKeyDecoder = org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
                .jwtDecoder(publicJwkSource);

        Clock clock = Clock.systemUTC();
        Instant issuedAt = clock.instant()
                .truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plusSeconds(300);

        JwsHeader header = JwsHeader
                .with(SignatureAlgorithm.RS256)
                .keyId(KEY_ID)
                .build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject("inventory-service")
                .audience(List.of("cinema-api"))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        Jwt encoded = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims));
        Jwt decoded = publicKeyDecoder.decode(
                encoded.getTokenValue());

        assertThat(decoded.getSubject())
                .isEqualTo("inventory-service");
        assertThat(decoded.getAudience())
                .containsExactly("cinema-api");
        assertThat(decoded.getIssuedAt()).isEqualTo(issuedAt);
        assertThat(decoded.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(decoded.getHeaders())
                .containsEntry("alg", "RS256")
                .containsEntry("kid", KEY_ID);
    }
}
