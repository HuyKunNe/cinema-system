package com.cinema.user.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import com.cinema.user.security.jwt.JwtSigningKeyLoader;
import com.cinema.user.security.jwt.RsaSigningKeyPair;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        AuthorizationServerProperties.class,
        JwtSigningKeyProperties.class
})
public class AuthorizationServerConfiguration {

    @Bean
    AuthorizationServerSettings authorizationServerSettings(
            AuthorizationServerProperties properties) {

        return AuthorizationServerSettings.builder()
                .issuer(properties.issuer())
                .build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "cinema.user.authorization-server.signing", name = "enabled", havingValue = "true")
    JWKSource<SecurityContext> jwkSource(
            JwtSigningKeyProperties properties,
            JwtSigningKeyLoader keyLoader) {

        RsaSigningKeyPair keyPair = keyLoader.load(properties);

        RSAKey rsaKey = new RSAKey.Builder(keyPair.publicKey())
                .privateKey(keyPair.privateKey())
                .keyID(properties.keyId())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .build();

        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }
}
