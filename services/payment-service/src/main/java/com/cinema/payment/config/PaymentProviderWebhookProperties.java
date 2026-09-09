package com.cinema.payment.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "cinema.payment.webhook")
public record PaymentProviderWebhookProperties(
        @NotNull(message = "cinema.payment.webhook.maximum-body-size is required")
                DataSize maximumBodySize) {

    @AssertTrue(message = "cinema.payment.webhook.maximum-body-size must be greater than zero")
    public boolean isMaximumBodySizeValid() {
        return maximumBodySize == null || maximumBodySize.toBytes() > 0;
    }
}
