package com.cinema.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

public class AuthorizationServerPropertiesTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:8082",
            "https://auth.cinema.example.com"
    })
    void shouldAcceptCanonicalIssuer(String issuer) {
        AuthorizationServerProperties properties = new AuthorizationServerProperties(issuer);

        assertThat(properties.isIssuerValid()).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            " ",
            "localhost:8082",
            "ftp://auth.cinema.example.com",
            "http://localhost:8082/",
            "http://localhost:8082/oauth2",
            "http://user:password@localhost:8082",
            "http://localhost:8082?environment=local",
            "http://localhost:8082#fragment"
    })
    void shouldRejectInvalidIssuer(String issuer) {
        AuthorizationServerProperties properties = new AuthorizationServerProperties(issuer);

        assertThat(properties.isIssuerValid()).isFalse();
    }
}
