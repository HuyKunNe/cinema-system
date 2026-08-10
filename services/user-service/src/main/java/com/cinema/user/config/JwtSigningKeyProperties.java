package com.cinema.user.config;

import java.util.regex.Pattern;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;

@Validated
@ConfigurationProperties(prefix = "cinema.user.authorization-server.signing")
public record JwtSigningKeyProperties(
        boolean enabled,
        String keyId,
        String privateKeyLocation,
        String publicKeyLocation) {
    private static final Pattern KEY_ID_PATTERN = Pattern.compile(
            "^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$");

    @AssertTrue(message = "JWT signing-key configuration is invalid")
    public boolean isConfigurationValid() {
        if (!enabled) {
            return true;
        }

        return hasText(keyId)
                && KEY_ID_PATTERN
                        .matcher(keyId)
                        .matches()
                && hasText(privateKeyLocation)
                && hasText(publicKeyLocation);
    }

    private static boolean hasText(String value) {
        return value != null
                && !value.isBlank();
    }
}
