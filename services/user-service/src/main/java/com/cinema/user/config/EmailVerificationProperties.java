package com.cinema.user.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "cinema.user.email-verification")
public record EmailVerificationProperties(

        @NotNull @DefaultValue("24h") Duration lifetime) {

    @AssertTrue(message = "Email verification lifetime must be positive")
    public boolean isLifetimeValid() {
        return lifetime != null
                && !lifetime.isZero()
                && !lifetime.isNegative();
    }
}
