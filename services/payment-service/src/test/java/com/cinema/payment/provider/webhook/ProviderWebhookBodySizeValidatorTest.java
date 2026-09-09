package com.cinema.payment.provider.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.config.PaymentProviderWebhookProperties;
import com.cinema.payment.exception.PaymentErrorCode;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class ProviderWebhookBodySizeValidatorTest {

    private final ProviderWebhookBodySizeValidator validator =
            new ProviderWebhookBodySizeValidator(
                    new PaymentProviderWebhookProperties(DataSize.ofBytes(8)));

    @Test
    void validateShouldAcceptBodyAtConfiguredLimit() {
        byte[] rawBody = new byte[8];

        assertThatCode(() -> validator.validate(rawBody)).doesNotThrowAnyException();
    }

    @Test
    void validateShouldRejectBodyAboveConfiguredLimit() {
        byte[] rawBody = new byte[9];

        assertThatThrownBy(() -> validator.validate(rawBody))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode.PROVIDER_WEBHOOK_BODY_TOO_LARGE));
    }

    @Test
    void validateShouldRejectNullBody() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode.PROVIDER_WEBHOOK_BODY_REQUIRED));
    }

    @Test
    void validateShouldRejectEmptyBody() {
        assertThatThrownBy(() -> validator.validate(new byte[0]))
                .isInstanceOf(ValidationException.class)
                .satisfies(
                        throwable ->
                                assertThat(((ValidationException) throwable).getErrorCode())
                                        .isEqualTo(
                                                PaymentErrorCode.PROVIDER_WEBHOOK_BODY_REQUIRED));
    }
}
