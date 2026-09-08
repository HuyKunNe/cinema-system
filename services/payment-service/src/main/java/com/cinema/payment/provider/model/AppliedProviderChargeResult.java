package com.cinema.payment.provider.model;

import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;

import java.util.UUID;

public record AppliedProviderChargeResult(
        UUID paymentId,
        UUID transactionId,
        PaymentStatus paymentStatus,
        PaymentTransactionStatus transactionStatus) {}
