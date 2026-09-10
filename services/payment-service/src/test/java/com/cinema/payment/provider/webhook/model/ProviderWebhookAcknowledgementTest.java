package com.cinema.payment.provider.webhook.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.exception.exception.InternalServerException;
import com.cinema.payment.exception.PaymentErrorCode;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

class ProviderWebhookAcknowledgementTest {

    @Test
    void noContentShouldCreateEmptyAcknowledgement() {
        ProviderWebhookAcknowledgement acknowledgement =
                ProviderWebhookAcknowledgement.noContent();

        assertThat(acknowledgement.status())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(acknowledgement.contentType()).isNull();
        assertThat(acknowledgement.body()).isEmpty();
    }

    @Test
    void bodyShouldBeDefensivelyCopied() {
        byte[] body =
                "{\"RspCode\":\"00\"}"
                        .getBytes(StandardCharsets.UTF_8);

        ProviderWebhookAcknowledgement acknowledgement =
                new ProviderWebhookAcknowledgement(
                        HttpStatus.OK,
                        MediaType.APPLICATION_JSON,
                        body);

        body[0] = 'X';

        byte[] returnedBody = acknowledgement.body();
        returnedBody[0] = 'Y';

        assertThat(
                        new String(
                                acknowledgement.body(),
                                StandardCharsets.UTF_8))
                .isEqualTo("{\"RspCode\":\"00\"}");
    }

    @Test
    void nonEmptyBodyWithoutContentTypeShouldBeRejected() {
        assertThatThrownBy(
                        () ->
                                new ProviderWebhookAcknowledgement(
                                        HttpStatus.OK,
                                        null,
                                        new byte[] {1}))
                .isInstanceOf(InternalServerException.class)
                .satisfies(
                        throwable ->
                                assertThat(
                                                ((InternalServerException) throwable)
                                                        .getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode
                                                        .PROVIDER_WEBHOOK_ACKNOWLEDGEMENT_INVALID));
    }

    @Test
    void failureStatusShouldBeRejected() {
        assertThatThrownBy(
                        () ->
                                new ProviderWebhookAcknowledgement(
                                        HttpStatus.BAD_REQUEST,
                                        MediaType.APPLICATION_JSON,
                                        new byte[] {1}))
                .isInstanceOf(InternalServerException.class);
    }
}
