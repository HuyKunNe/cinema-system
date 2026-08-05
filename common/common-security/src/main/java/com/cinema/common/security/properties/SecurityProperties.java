package com.cinema.common.security.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cinema.security.oauth2")
public record SecurityProperties(
        String issuerUri,
        String jwkSetUri,
        String audience) {
}
