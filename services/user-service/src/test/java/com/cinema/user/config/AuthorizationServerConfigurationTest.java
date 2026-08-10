package com.cinema.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

public class AuthorizationServerConfigurationTest {
    @Test
    void shouldCreateSettingsWithConfiguredIssuer() {
        AuthorizationServerConfiguration configuration = new AuthorizationServerConfiguration();

        AuthorizationServerSettings settings = configuration.authorizationServerSettings(
                new AuthorizationServerProperties(
                        "https://auth.cinema.example.com"));

        assertThat(settings.getIssuer())
                .isEqualTo("https://auth.cinema.example.com");

        assertThat(settings.getAuthorizationEndpoint())
                .isEqualTo("/oauth2/authorize");

        assertThat(settings.getTokenEndpoint())
                .isEqualTo("/oauth2/token");

        assertThat(settings.getJwkSetEndpoint())
                .isEqualTo("/oauth2/jwks");

        assertThat(settings.getOidcUserInfoEndpoint())
                .isEqualTo("/userinfo");
    }
}
