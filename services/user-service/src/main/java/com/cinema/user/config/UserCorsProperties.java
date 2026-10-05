package com.cinema.user.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

@Validated
@ConfigurationProperties(prefix = "cinema.user.cors")
public record UserCorsProperties(
        @DefaultValue({
                    "http://localhost:5173",
                    "http://localhost:8081",
                    "http://localhost:8083",
                    "http://localhost:8084",
                    "http://localhost:8085"
                })
                @NotEmpty
                List<@NotBlank String> allowedOrigins) {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    @AssertTrue(
            message = "CORS allowed origins must be explicit HTTP or HTTPS origins without paths")
    public boolean isAllowedOriginsValid() {
        return allowedOrigins != null
                && !allowedOrigins.isEmpty()
                && allowedOrigins.stream().allMatch(UserCorsProperties::isValidOrigin);
    }

    private static boolean isValidOrigin(String origin) {
        if (origin == null || origin.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(origin);

            return uri.isAbsolute()
                    && ALLOWED_SCHEMES.contains(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getUserInfo() == null
                    && uri.getQuery() == null
                    && uri.getFragment() == null
                    && (uri.getPath() == null || uri.getPath().isEmpty())
                    && uri.getPort() >= -1
                    && uri.getPort() <= 65535;
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
