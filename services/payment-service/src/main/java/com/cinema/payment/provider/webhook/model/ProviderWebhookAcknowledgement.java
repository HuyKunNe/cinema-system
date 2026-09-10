package com.cinema.payment.provider.webhook.model;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.payment.exception.PaymentErrorCode;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

public record ProviderWebhookAcknowledgement(
        HttpStatus status, MediaType contentType, byte[] body) {

    public ProviderWebhookAcknowledgement {

        if (status == null
                || !status.is2xxSuccessful()
                || body == null
                || (body.length > 0 && contentType == null)) {

            throw new InternalServerException(
                    PaymentErrorCode.PROVIDER_WEBHOOK_ACKNOWLEDGEMENT_INVALID);
        }

        body = body.clone();
    }

    @Override
    public byte[] body() {
        return body.clone();
    }

    public static ProviderWebhookAcknowledgement noContent() {
        return new ProviderWebhookAcknowledgement(HttpStatus.NO_CONTENT, null, new byte[0]);
    }
}
