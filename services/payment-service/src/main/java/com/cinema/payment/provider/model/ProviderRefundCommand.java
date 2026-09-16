package com.cinema.payment.provider.model;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

public record ProviderRefundCommand(
        UUID paymentId, String chargeProviderReference, BigDecimal amount, String currency) {

    public ProviderRefundCommand {

        if (paymentId == null) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_ID_REQUIRED);
        }

        if (chargeProviderReference == null || chargeProviderReference.isBlank()) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_REFERENCE_REQUIRED);
        }

        chargeProviderReference = chargeProviderReference.strip();

        if (chargeProviderReference.length() > 255) {
            throw new ValidationException(PaymentErrorCode.PROVIDER_RESULT_INVALID);
        }

        if (amount == null) {
            throw new ValidationException(PaymentErrorCode.AMOUNT_REQUIRED);
        }

        if (amount.signum() <= 0) {
            throw new ValidationException(PaymentErrorCode.AMOUNT_INVALID);
        }

        if (currency == null || currency.isBlank()) {
            throw new ValidationException(PaymentErrorCode.CURRENCY_REQUIRED);
        }

        String normalizedCurrency = currency.strip().toUpperCase(Locale.ROOT);

        if (normalizedCurrency.length() != 3
                || !normalizedCurrency.chars().allMatch(Character::isLetter)) {

            throw new ValidationException(PaymentErrorCode.CURRENCY_INVALID);
        }

        currency = normalizedCurrency;
    }
}
