package com.cinema.user.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@Validated
@ConfigurationProperties(
        prefix = "cinema.user.authorization-server.jwt")
public record JwtClaimsProperties(
        @NotEmpty
        List<@NotBlank String> audiences) {

    public JwtClaimsProperties {
        audiences = audiences == null
                ? List.of()
                : List.copyOf(audiences);
    }
}
