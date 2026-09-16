package com.cinema.payment.provider.model;

import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.RefundStatus;

import java.util.UUID;

public record AppliedProviderRefundResult(
        UUID paymentId,
        UUID transactionId,
        RefundStatus refundStatus,
        PaymentTransactionStatus transactionStatus) {}
