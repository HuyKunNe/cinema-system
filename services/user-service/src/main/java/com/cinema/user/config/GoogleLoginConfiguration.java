package com.cinema.user.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GoogleLoginProperties.class)
public class GoogleLoginConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "cinema.user.google", name = "enabled", havingValue = "true")
    ClientRegistrationRepository googleClientRegistrationRepository(
            GoogleLoginProperties properties) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(
                    "Google login is enabled but USER_GOOGLE_CLIENT_ID or USER_GOOGLE_CLIENT_SECRET"
                        + " is missing");
        }

        ClientRegistration registration =
                CommonOAuth2Provider.GOOGLE
                        .getBuilder("google")
                        .clientId(properties.clientId())
                        .clientSecret(properties.clientSecret())
                        .scope("openid", "profile", "email")
                        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                        .build();

        return new InMemoryClientRegistrationRepository(registration);
    }
}
