package com.cinema.user.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "cinema.user.authorization-server")
public record AuthorizationServerProperties(

        @NotBlank String issuer) {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    @AssertTrue(message = "Authorization Server issuer must be a canonical HTTP or HTTPS URL")
    public boolean isIssuerValid() {
        if (issuer == null || issuer.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(issuer);

            return uri.isAbsolute()
                    && ALLOWED_SCHEMES.contains(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getUserInfo() == null
                    && uri.getQuery() == null
                    && uri.getFragment() == null
                    && isRootPath(uri.getPath())
                    && !issuer.endsWith("/");
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private static boolean isRootPath(String path) {
        return path == null || path.isEmpty();
    }
}
