package com.cinema.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cinema.user.google")
public record GoogleLoginProperties(boolean enabled, String clientId, String clientSecret) {

    public boolean isConfigured() {
        return enabled
                && clientId != null
                && !clientId.isBlank()
                && clientSecret != null
                && !clientSecret.isBlank();
    }
}
