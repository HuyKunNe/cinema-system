package com.cinema.payment.controller.response;

import com.cinema.payment.enums.PaymentTransactionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record PaymentTransactionManagementResponse(
        UUID id,
        UUID paymentId,
        String provider,
                String transactionType,
                Integer attemptNumber,
        PaymentTransactionStatus status,
        BigDecimal amount,
        String currency,
        String providerReference,
        String failureCode,
        OffsetDateTime requestedAt,
        OffsetDateTime completedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {}
