package com.cinema.payment.controller.response;

import com.cinema.payment.enums.PaymentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentCheckoutResponse(
        UUID bookingId,
        UUID paymentId,
        PaymentStatus status,
        String checkoutUri,
        OffsetDateTime holdExpiresAt) {}
