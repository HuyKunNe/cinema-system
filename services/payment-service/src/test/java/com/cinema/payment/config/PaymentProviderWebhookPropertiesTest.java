package com.cinema.payment.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.util.Set;

class PaymentProviderWebhookPropertiesTest {

    @Test
    void positiveMaximumBodySizeShouldBeValid() {
        PaymentProviderWebhookProperties properties =
                new PaymentProviderWebhookProperties(DataSize.ofKilobytes(64));

        Set<ConstraintViolation<PaymentProviderWebhookProperties>> violations =
                validate(properties);

        assertThat(violations).isEmpty();
    }

    @Test
    void zeroMaximumBodySizeShouldBeInvalid() {
        PaymentProviderWebhookProperties properties =
                new PaymentProviderWebhookProperties(DataSize.ofBytes(0));

        Set<ConstraintViolation<PaymentProviderWebhookProperties>> violations =
                validate(properties);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("cinema.payment.webhook.maximum-body-size must be greater than zero");
    }

    @Test
    void missingMaximumBodySizeShouldBeInvalid() {
        PaymentProviderWebhookProperties properties = new PaymentProviderWebhookProperties(null);

        Set<ConstraintViolation<PaymentProviderWebhookProperties>> violations =
                validate(properties);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("cinema.payment.webhook.maximum-body-size is required");
    }

    private Set<ConstraintViolation<PaymentProviderWebhookProperties>> validate(
            PaymentProviderWebhookProperties properties) {
        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();
            return validator.validate(properties);
        }
    }
}
