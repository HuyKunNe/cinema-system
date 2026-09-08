package com.cinema.payment.provider.model;

import com.cinema.common.exception.exception.ValidationException;
import com.cinema.payment.exception.PaymentErrorCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

public record ProviderChargeCommand(
        UUID paymentId,
        UUID bookingId,
        BigDecimal amount,
        String currency,
        OffsetDateTime holdExpiresAt) {

    public ProviderChargeCommand {

        if (paymentId == null) {
            throw new ValidationException(PaymentErrorCode.PAYMENT_ID_REQUIRED);
        }

        if (bookingId == null) {
            throw new ValidationException(PaymentErrorCode.BOOKING_ID_REQUIRED);
        }

        if (amount == null) {
            throw new ValidationException(PaymentErrorCode.AMOUNT_REQUIRED);
        }

        if (amount.signum() < 0) {
            throw new ValidationException(PaymentErrorCode.AMOUNT_INVALID);
        }

        if (currency == null || currency.isBlank()) {
            throw new ValidationException(PaymentErrorCode.CURRENCY_REQUIRED);
        }

        String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);

        if (normalizedCurrency.length() != 3
                || !normalizedCurrency.chars().allMatch(Character::isLetter)) {

            throw new ValidationException(PaymentErrorCode.CURRENCY_INVALID);
        }

        if (holdExpiresAt == null) {
            throw new ValidationException(PaymentErrorCode.HOLD_EXPIRATION_REQUIRED);
        }

        currency = normalizedCurrency;
    }
}
