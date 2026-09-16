package com.cinema.payment.model;

import com.cinema.payment.enums.FinancialAuditActorType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RefundRequest(
        UUID paymentId,
        FinancialAuditActorType actorType,
        String actorId,
        String reason,
        UUID correlationId,
        OffsetDateTime requestedAt) {}
