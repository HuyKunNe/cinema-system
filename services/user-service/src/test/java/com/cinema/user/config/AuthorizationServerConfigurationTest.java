package com.cinema.user.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import com.cinema.user.oauth2.token.RefreshTokenReuseService;
import com.cinema.user.oauth2.token.RefreshTokenTrackingService;
import com.cinema.user.oauth2.token.TrackingOAuth2AuthorizationService;
import com.cinema.user.security.jwt.JwtSigningKeyLoader;
import com.cinema.user.security.jwt.RsaSigningKeyPair;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

class AuthorizationServerConfigurationTest {

    private static final String ISSUER = "https://auth.cinema.example.com";
    private static final String KEY_ID = "cinema-user-test-2026-01";

    private static RsaSigningKeyPair signingKeyPair;

    @BeforeAll
    static void generateSigningKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);

        KeyPair keyPair = generator.generateKeyPair();
        signingKeyPair = new RsaSigningKeyPair(
                (RSAPublicKey) keyPair.getPublic(),
                (RSAPrivateKey) keyPair.getPrivate());
    }

    @Test
    void shouldCreateSettingsWithConfiguredIssuer() {
        AuthorizationServerConfiguration configuration = new AuthorizationServerConfiguration();

        AuthorizationServerSettings settings = configuration.authorizationServerSettings(
                new AuthorizationServerProperties(ISSUER));

        assertThat(settings.getIssuer()).isEqualTo(ISSUER);
        assertThat(settings.getAuthorizationEndpoint())
                .isEqualTo("/oauth2/authorize");
        assertThat(settings.getTokenEndpoint())
                .isEqualTo("/oauth2/token");
        assertThat(settings.getJwkSetEndpoint())
                .isEqualTo("/oauth2/jwks");
        assertThat(settings.getOidcUserInfoEndpoint())
                .isEqualTo("/userinfo");
    }

    @Test
    void shouldRegisterAuthorizationServerProperties() {
        disabledSigningContext().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AuthorizationServerProperties.class);
            assertThat(context.getBean(AuthorizationServerProperties.class).issuer())
                    .isEqualTo(ISSUER);
        });
    }

    @Test
    void shouldRegisterJwtSigningKeyProperties() {
        disabledSigningContext().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(JwtSigningKeyProperties.class);
            assertThat(context.getBean(JwtSigningKeyProperties.class).enabled())
                    .isFalse();
        });
    }

    @Test
    void shouldNotRegisterJwkSourceWhenSigningIsDisabled() {
        JwtSigningKeyLoader keyLoader = mock(JwtSigningKeyLoader.class);

        disabledSigningContext()
                .withBean(JwtSigningKeyLoader.class, () -> keyLoader)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(JWKSource.class);
                    assertThat(context).doesNotHaveBean(JwtEncoder.class);
                    assertThat(context).doesNotHaveBean(JwtDecoder.class);
                    verifyNoInteractions(keyLoader);
                });
    }

    @Test
    void shouldRegisterJwkSourceWhenSigningIsEnabled() {
        JwtSigningKeyLoader keyLoader = mock(JwtSigningKeyLoader.class);
        when(keyLoader.load(org.mockito.ArgumentMatchers.any(
                JwtSigningKeyProperties.class)))
                .thenReturn(signingKeyPair);

        enabledSigningContext(keyLoader).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(JWKSource.class);

            verify(keyLoader).load(context.getBean(JwtSigningKeyProperties.class));
        });
    }

    @Test
    void shouldConfigureRsaJwkWithApprovedMetadata() {
        JwtSigningKeyLoader keyLoader = mock(JwtSigningKeyLoader.class);
        when(keyLoader.load(org.mockito.ArgumentMatchers.any(
                JwtSigningKeyProperties.class)))
                .thenReturn(signingKeyPair);

        enabledSigningContext(keyLoader).run(context -> {
            assertThat(context).hasNotFailed();

            RSAKey rsaKey = getOnlyRsaKey(context.getBean(JWKSource.class));

            assertThat(rsaKey.getKeyID()).isEqualTo(KEY_ID);
            assertThat(rsaKey.getKeyUse()).isEqualTo(KeyUse.SIGNATURE);
            assertThat(rsaKey.getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
            assertThat(rsaKey.toRSAPublicKey())
                    .isEqualTo(signingKeyPair.publicKey());
            assertThat(rsaKey.toRSAPrivateKey())
                    .isEqualTo(signingKeyPair.privateKey());
        });
    }

    @Test
    void publicJwkShouldNotExposePrivateKeyMaterial() {
        JwtSigningKeyLoader keyLoader = mock(JwtSigningKeyLoader.class);
        when(keyLoader.load(org.mockito.ArgumentMatchers.any(
                JwtSigningKeyProperties.class)))
                .thenReturn(signingKeyPair);

        enabledSigningContext(keyLoader).run(context -> {
            RSAKey internalKey = getOnlyRsaKey(context.getBean(JWKSource.class));
            JWK publicKey = internalKey.toPublicJWK();

            assertThat(internalKey.isPrivate()).isTrue();
            assertThat(publicKey.isPrivate()).isFalse();
            assertThat(publicKey.getKeyID()).isEqualTo(KEY_ID);
            assertThat(publicKey.getAlgorithm()).isEqualTo(JWSAlgorithm.RS256);
            assertThat(publicKey.getKeyUse()).isEqualTo(KeyUse.SIGNATURE);
        });
    }

    @Test
    void shouldRegisterJwtEncoderAndDecoderWhenSigningIsEnabled() {
        JwtSigningKeyLoader keyLoader = mock(JwtSigningKeyLoader.class);

        when(keyLoader.load(org.mockito.ArgumentMatchers.any(
                JwtSigningKeyProperties.class)))
                .thenReturn(signingKeyPair);

        enabledSigningContext(keyLoader).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(JwtEncoder.class);
            assertThat(context).hasSingleBean(JwtDecoder.class);
        });
    }

    @Test
    void shouldEncodeAndDecodeJwtUsingConfiguredRsaKeyPair() {
        JwtSigningKeyLoader keyLoader = mock(JwtSigningKeyLoader.class);

        when(keyLoader.load(org.mockito.ArgumentMatchers.any(
                JwtSigningKeyProperties.class)))
                .thenReturn(signingKeyPair);

        enabledSigningContext(keyLoader).run(context -> {
            assertThat(context).hasNotFailed();

            JwtEncoder encoder = context.getBean(JwtEncoder.class);
            JwtDecoder decoder = context.getBean(JwtDecoder.class);

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
                    .subject("0198-aaaa-bbbb-7ccc-8ddd-eeeeeeeeeeee")
                    .audience(List.of("cinema-api"))
                    .issuedAt(issuedAt)
                    .expiresAt(expiresAt)
                    .claim("username", "customer@example.com")
                    .build();

            Jwt encoded = encoder.encode(
                    JwtEncoderParameters.from(header, claims));

            Jwt decoded = decoder.decode(encoded.getTokenValue());

            assertThat(encoded.getHeaders())
                    .containsEntry("alg", SignatureAlgorithm.RS256)
                    .containsEntry("kid", KEY_ID);

            assertThat(decoded.getIssuer().toString())
                    .isEqualTo(ISSUER);
            assertThat(decoded.getSubject())
                    .isEqualTo("0198-aaaa-bbbb-7ccc-8ddd-eeeeeeeeeeee");
            assertThat(decoded.getAudience())
                    .containsExactly("cinema-api");
            assertThat(decoded.getClaimAsString("username"))
                    .isEqualTo("customer@example.com");
            assertThat(decoded.getIssuedAt())
                    .isEqualTo(issuedAt);
            assertThat(decoded.getExpiresAt())
                    .isEqualTo(expiresAt);
        });
    }

    @Test
    void shouldRegisterTrackingAuthorizationService() {
        disabledSigningContext()
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed();

                    assertThat(context)
                            .hasSingleBean(
                                    OAuth2AuthorizationService.class);

                    assertThat(context.getBean(
                            OAuth2AuthorizationService.class))
                            .isInstanceOf(
                                    TrackingOAuth2AuthorizationService.class);
                });
    }

    private ApplicationContextRunner disabledSigningContext() {
        return baseContext()
                .withPropertyValues(
                        "cinema.user.authorization-server.signing.enabled=false");
    }

    private ApplicationContextRunner enabledSigningContext(
            JwtSigningKeyLoader keyLoader) {

        return baseContext()
                .withBean(JwtSigningKeyLoader.class, () -> keyLoader)
                .withPropertyValues(
                        "cinema.user.authorization-server.signing.enabled=true",
                        "cinema.user.authorization-server.signing.key-id=" + KEY_ID,
                        "cinema.user.authorization-server.signing.private-key-location="
                                + "file:/unused/private.pem",
                        "cinema.user.authorization-server.signing.public-key-location="
                                + "file:/unused/public.pem");
    }

    private ApplicationContextRunner baseContext() {
        return new ApplicationContextRunner()
                .withUserConfiguration(
                        AuthorizationServerConfiguration.class)
                .withBean(
                        JdbcOperations.class,
                        () -> mock(
                                JdbcOperations.class))
                .withBean(
                        RegisteredClientRepository.class,
                        () -> mock(
                                RegisteredClientRepository.class))
                .withBean(
                        RefreshTokenTrackingService.class,
                        () -> mock(
                                RefreshTokenTrackingService.class))
                .withBean(
                        RefreshTokenReuseService.class,
                        () -> mock(
                                RefreshTokenReuseService.class))
                .withPropertyValues(
                        "cinema.user.authorization-server.issuer="
                                + ISSUER,
                        "cinema.user.authorization-server.jwt.audiences=cinema-api");
    }

    @SuppressWarnings("unchecked")
    private RSAKey getOnlyRsaKey(JWKSource<?> source) throws Exception {
        JWKSource<SecurityContext> typedSource = (JWKSource<SecurityContext>) source;

        List<JWK> keys = typedSource.get(
                new JWKSelector(new JWKMatcher.Builder().build()),
                null);

        assertThat(keys).hasSize(1);
        assertThat(keys.getFirst()).isInstanceOf(RSAKey.class);

        return (RSAKey) keys.getFirst();
    }
}
