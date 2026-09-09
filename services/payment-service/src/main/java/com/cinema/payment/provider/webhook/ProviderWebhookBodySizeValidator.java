package com.cinema.payment.provider.webhook;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.config.PaymentProviderWebhookProperties;
import com.cinema.payment.exception.PaymentErrorCode;

import org.springframework.stereotype.Component;

@Component
public class ProviderWebhookBodySizeValidator {

    private final long maximumBodySizeInBytes;

    public ProviderWebhookBodySizeValidator(PaymentProviderWebhookProperties properties) {
        this.maximumBodySizeInBytes = properties.maximumBodySize().toBytes();
    }

    public void validate(byte[] rawBody) {
        if (rawBody == null || rawBody.length == 0) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_BODY_REQUIRED);
        }

        if (rawBody.length > maximumBodySizeInBytes) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_WEBHOOK_BODY_TOO_LARGE);
        }
    }
}
