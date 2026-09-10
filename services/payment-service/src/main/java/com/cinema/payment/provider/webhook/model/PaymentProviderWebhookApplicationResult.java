package com.cinema.payment.provider.webhook.model;

import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentTransactionStatus;

import java.util.UUID;

public record PaymentProviderWebhookApplicationResult(
        UUID paymentId,
        UUID paymentTransactionId,
        PaymentStatus paymentStatus,
        PaymentTransactionStatus transactionStatus,
        ProviderWebhookApplicationDisposition disposition) {}
