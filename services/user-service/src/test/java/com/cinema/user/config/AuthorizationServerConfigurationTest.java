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
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

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
        AuthorizationServerConfiguration configuration =
                new AuthorizationServerConfiguration();

        AuthorizationServerSettings settings =
                configuration.authorizationServerSettings(
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
                .withUserConfiguration(AuthorizationServerConfiguration.class)
                .withPropertyValues(
                        "cinema.user.authorization-server.issuer=" + ISSUER);
    }

    @SuppressWarnings("unchecked")
    private RSAKey getOnlyRsaKey(JWKSource<?> source) throws Exception {
        JWKSource<SecurityContext> typedSource =
                (JWKSource<SecurityContext>) source;

        List<JWK> keys = typedSource.get(
                new JWKSelector(new JWKMatcher.Builder().build()),
                null);

        assertThat(keys).hasSize(1);
        assertThat(keys.getFirst()).isInstanceOf(RSAKey.class);

        return (RSAKey) keys.getFirst();
    }
}
