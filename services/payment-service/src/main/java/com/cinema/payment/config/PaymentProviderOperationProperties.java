package com.cinema.payment.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "cinema.payment.provider-operation")
public record PaymentProviderOperationProperties(
        @Min(1) @Max(100) int batchSize, @NotNull Duration leaseDuration) {

    @AssertTrue(
            message =
                    "cinema.payment.provider-operation.lease-duration "
                            + "must be greater than zero")
    public boolean isLeaseDurationValid() {

        return leaseDuration == null || (!leaseDuration.isZero() && !leaseDuration.isNegative());
    }
}
