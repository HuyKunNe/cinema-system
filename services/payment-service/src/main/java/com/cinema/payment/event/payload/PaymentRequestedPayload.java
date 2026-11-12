package com.cinema.payment.event.payload;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentRequestedPayload(
        UUID bookingId,
        UUID userId,
        BigDecimal amount,
        String currency,
        int paymentAttempt,
        OffsetDateTime holdExpiresAt,
        OffsetDateTime requestedAt,
        String paymentProvider) {

    public PaymentRequestedPayload(
            UUID bookingId,
            UUID userId,
            BigDecimal amount,
            String currency,
            int paymentAttempt,
            OffsetDateTime holdExpiresAt,
            OffsetDateTime requestedAt) {
        this(bookingId, userId, amount, currency, paymentAttempt, holdExpiresAt, requestedAt, null);
    }
}
