package com.cinema.payment.model;

import com.cinema.payment.enums.PaymentTransactionStatus;
import com.cinema.payment.enums.ReconciliationResolution;
import com.cinema.payment.enums.ReconciliationStatus;
import com.cinema.payment.enums.RefundStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReconciliationOperationResult(
        UUID reconciliationCaseId,
        UUID paymentId,
        UUID paymentTransactionId,
        ReconciliationStatus reconciliationStatus,
        ReconciliationResolution resolution,
        RefundStatus refundStatus,
        PaymentTransactionStatus transactionStatus,
        OffsetDateTime resolvedAt) {}
